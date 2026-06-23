# Plugin Architecture

Activity Launcher supports external plugins for shortcut creation and activity launching. This document describes the intent-based protocol used to communicate with these plugins.

## Shortcut Creation Flow

When a user requests to create a shortcut, Activity Launcher sends a `CREATE_SHORTCUT` intent. A plugin can register for this action to handle the shortcut creation (e.g., to provide custom icons, labels, or to use a different shortcut pinning mechanism).

### Intent: `activitylauncher.intent.action.CREATE_SHORTCUT`

| Extra | Type | Description |
|---|---|---|
| `extra_name` | String | The suggested name for the shortcut. |
| `extra_icon` | Bundle | The icon as a `IconCompat` bundle. |
| `extra_intent` | String | The target intent encoded as a URI. |
| `sign` | String | The signature of the target intent URI. |
| `extra_shortcut_activity` | String | Flattened `ComponentName` of AL's `ShortcutActivity`. |
| `extra_launch_plugin` | String | (Optional) Flattened `ComponentName` of a plugin to handle the actual launch. |

### Sequence Diagram

```mermaid
sequenceDiagram
    participant AL as Activity Launcher
    participant Plugin as Shortcut Plugin
    participant System as Android System
    participant Home as Home Screen

    AL->>AL: Sign Target Intent
    AL->>System: Start Activity (CREATE_SHORTCUT)
    System->>Plugin: Receive Intent
    Plugin->>Plugin: Construct LAUNCH_SHORTCUT Intent
    Note over Plugin: Intent includes extra_intent, sign, and extra_launch_plugin
    Plugin->>System: requestPinShortcut(LAUNCH_SHORTCUT Intent)
    System-->>Home: Shortcut Created
```

## Shortcut Launch Flow

When a shortcut created by a plugin is clicked, it must launch Activity Launcher's `ShortcutActivity` with the `LAUNCH_SHORTCUT` action.

### Intent: `activitylauncher.intent.action.LAUNCH_SHORTCUT`

| Extra | Type | Description |
|---|---|---|
| `extra_intent` | String | The target intent URI (received from `CREATE_SHORTCUT`). |
| `sign` | String | The signature (received from `CREATE_SHORTCUT`). |
| `extra_launch_plugin` | String | (Optional) Flattened `ComponentName` of a plugin to handle the actual launch. |

### Sequence Diagram

```mermaid
sequenceDiagram
    participant User
    participant Home as Home Screen
    participant AL as ShortcutActivity
    participant LP as Launch Plugin
    participant Target as Target Activity

    User->>Home: Click Shortcut
    Home->>AL: Start Activity (LAUNCH_SHORTCUT)
    AL->>AL: Validate Signature
    alt Has extra_launch_plugin
        AL->>LP: Start Activity (LAUNCH_ACTIVITY)
        LP->>Target: Start Activity
    else Default Launch
        AL->>Target: Start Activity
    end
```

## Launch Delegation

If the `LAUNCH_SHORTCUT` intent contains an `extra_launch_plugin` extra, `ShortcutActivity` will validate the signature and then delegate the launch to the specified component.

### Intent: `activitylauncher.intent.action.LAUNCH_ACTIVITY`

Sent to the component specified in `extra_launch_plugin`.

| Extra | Type | Description |
|---|---|---|
| `extra_intent` | String | The validated target intent encoded as a URI. |

If no `extra_launch_plugin` is provided, Activity Launcher performs the default launch using `context.startActivity()`.

## Intent Encoding

To ensure robustness and support for complex intents (including extras), Activity Launcher uses `Intent.toUri(Intent.URI_INTENT_SCHEME)` for encoding intents in the `extra_intent` field.

For backward compatibility, `ShortcutActivity` also supports parsing URIs created with the legacy `0` flag.
