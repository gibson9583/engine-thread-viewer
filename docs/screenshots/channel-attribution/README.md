# Web administrator screenshots

These screenshots show Thread Viewer inside the actual built OIE web
administrator: its navigation shell, dashboard dock, plugin registration,
styles, and Radix dialogs. The engine API replays unchanged snapshots from the
isolated OIE 4.6.0 validation run on September 22, 2026. All channel names and IDs
are synthetic test data; this is not a currently connected engine session.

- [Dashboard overview](web-dashboard-overview.png): compact filters, host-styled
  sortable headers, numeric cells, and small Details buttons alongside the
  existing dashboard controls.
- [Channel ownership](web-channel-ownership.png): the stable channel filter
  selects `TV Review JS Poll`, while `Channel ownership` identifies its idle
  polling scheduler. The snapshot remains available after monitoring
  stops.
- [Connector identity](web-connector-identity.png): `Send (1) Backup` stays intact
  as the connector name, separate from connector metadata ID `1`. The host's
  key/value detail grid shows the channel ID, association and match reason.
- [Refreshed details](web-refreshed-channel-details.png): an already-open dialog
  follows worker `101` from `TV Review Destination Queued` to `TV Review VM Slow`
  on the next API poll. The later sample shows the differing saved name,
  `Saved Rename Without Redeploy`.

Captures use Chromium at 1600×1000 with the real dashboard splitter expanding
its dock. There are no screenshot-only CSS overrides. Additional checks cover
its default 230px dock, 1000px and 800px viewports, both themes, keyboard sorting
and Details activation, long labels, empty results, and filter clearing. The
host Server Log tab supplies a direct styling reference. Detail timestamps use
the host's timezone-aware formatter.

Capture inputs (SHA-256):

| Input | SHA-256 |
| --- | --- |
| Shipped web bundle | `8f5e3c84b68694191e5cb1493c8cbd7ee5359984827c712537f6225e79f44557` |
| Active JSON snapshot | `74c3747a3ceec9f8dab09cd5849a03eb1943b3502d683fb1c85474ab58ab57d5` |
| Saved-rename JSON snapshot | `e0ec88e1ceeb9a8e2e40d8e145454747d82b1c63c6d3dcc16748c3d95fdc2e4b` |
| Deployed-idle JSON snapshot | `2771544a677120246779a35f0c22256e714adc22077511bc9a7d922647dc8fa3` |

The capture harness, original snapshots, assertions, host asset hashes, and
image hashes are retained with the local validation evidence. All owned browser
and web-server processes exit after capture; no engine or database is created.
