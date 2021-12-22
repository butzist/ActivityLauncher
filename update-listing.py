#!/usr/bin/env python
# coding: utf-8

import json
import os
import re
from glob import glob
from xml.dom.minidom import parse as parse_xml

import httplib2
from apiclient.discovery import build as build_service
from oauth2client.service_account import ServiceAccountCredentials

SERVICE_ACCOUNT_EMAIL = os.getenv("SERVICEACCOUNT_EMAIL")
SERVICE_ACCOUNT_KEYFILE = os.getenv("SERVICEACCOUNT")
PACKAGE_NAME = "de.szalkowski.activitylauncher"
SCOPE = "https://www.googleapis.com/auth/androidpublisher"


def get_service():
    keyfile = json.loads(SERVICE_ACCOUNT_KEYFILE)
    credentials = ServiceAccountCredentials.from_json_keyfile_dict(keyfile, scopes=[SCOPE])

    http = httplib2.Http()
    http = credentials.authorize(http)

    service = build_service("androidpublisher", "v3", http=http)

    return service


def parse_strings_resources(filename):
    with open(filename, "r") as fd:
        dom = parse_xml(fd)
    result = dict()
    for element in dom.childNodes[0].getElementsByTagName("string"):
        key = element.attributes.getNamedItem("name").value
        value = element.childNodes[0].data
        result[key] = value
    return result


def load_string_resources():
    regex = re.compile("-(.*)\/")
    strings = dict()

    for fname in glob("ActivityLauncherApp/src/main/res/values-*/strings.xml"):
        code = next(regex.finditer(fname))[1].split("-")
        if len(code) > 1:
            code[1] = code[1][1:]

        resources = parse_strings_resources(fname)
        strings[tuple(code)] = {
            "shortDescription": resources["short_description"],
            "title": resources["app_name"],
        }

    for key, value in list(strings.items()):
        if len(key) == 2 and key[:1] not in strings:
            strings[key[:1]] = value

    return strings


def find_strings(code, strings):
    code = tuple(code)
    if code in strings:
        return strings[code]
    elif code[:1] in strings:
        return strings[code[:1]]
    else:
        return strings[("en",)]


def update_listings(service, strings):
    # prepare edit
    edit_request = service.edits().insert(body={}, packageName=PACKAGE_NAME)
    result = edit_request.execute()
    edit_id = result["id"]

    # create edits
    regex = re.compile("-(.*)\.")
    for fname in glob("descriptions/description-*.txt"):
        code = next(regex.finditer(fname))[1].split("-")
        if len(code) > 2 or code[0] == "xxx":
            print(f"skipping {'-'.join(code[1:])}")
            continue

        with open(fname, "r") as fd:
            fullDescription = fd.read()

        doc = find_strings(code, strings)
        doc["fullDescription"] = fullDescription

        listing_response = (
            service.edits().listings().update(editId=edit_id, packageName=PACKAGE_NAME, language="-".join(code), body=doc).execute()
        )

    # commit edits
    commit_request = service.edits().commit(editId=edit_id, packageName=PACKAGE_NAME).execute()


if __name__ == "__main__":
    service = get_service()
    strings = load_string_resources()

    update_listings(service, strings)
