# LocalRock

LocalRock lets people control their Roborock robots over their own network, through a self-hosted Server instead of Roborock's cloud.

## Language

**Robot**:
A Roborock robot vacuum that has been added to the Server.
_Avoid_: Vacuum, device

**Server**:
The self-hosted `local_roborock_server` that stands in for Roborock's cloud, and the only thing the App and Web App talk to.
_Avoid_: Backend, cloud, local server

**Onboarding**:
Adding a new Robot to the Server, by joining the Robot's own Wi-Fi network and handing it the Server's details. Only the App can do this.
_Avoid_: Pairing, setup, adding a vacuum

**Feature Report**:
What a Robot says about itself when asked which features it has. It can be wrong in both directions: a Robot may claim a feature it then refuses, so only the Robot's actual answer settles whether it supports something.
_Avoid_: Feature flags, feature set, capabilities

**App**:
The LocalRock mobile client for Android and iOS.
_Avoid_: Mobile app, client

**Web App**:
The LocalRock client that runs in a browser on a computer or a phone, on the same home network as the Server, and looks nearly the same as the App. It can also be added to a phone's home screen and opened from there.
_Avoid_: Webapp, web client, localrobot

**Live View**:
Live video from a Robot's camera, watched in the Web App or the App.
_Avoid_: Camera, preview, video stream, remote view

**Remote-view PIN**:
The PIN, or drawn pattern, that a Robot asks for before it starts Live View.
_Avoid_: Camera password, homesec password

**Camera Activation**:
The one-time switch, done differently on each Robot model, that lets a Robot accept Live View at all. Without it, the Robot refuses Live View even with the right Remote-view PIN.
_Avoid_: Enabling the camera, remote viewing authorisation

**Wall**:
A real wall or other solid edge that a Robot found while mapping, shown on its map.
_Avoid_: Wall cell, obstacle

**Virtual Wall**:
A line drawn on a Robot's map that the Robot will not cross. It is not a Wall.
_Avoid_: Wall, barrier, no-go line

**Scan**:
A Robot's map as it actually measured the home: rooms in their real shape, with Walls, furniture outlines and gaps where it could not drive.
_Avoid_: Raw map, cell map

**Floor Plan**:
The tidy version of a Robot's map that some Robots send alongside the Scan: each room as a straight-edged shape, with outer walls, inner walls and doors marked on its edges.
_Avoid_: Squared rooms, polygons, zone lines
