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

**App**:
The LocalRock mobile client for Android and iOS.
_Avoid_: Mobile app, client

**Web App**:
The LocalRock client that runs in a browser on a computer or a phone, on the same home network as the Server, and looks nearly the same as the App. It can also be added to a phone's home screen and opened from there.
_Avoid_: Webapp, web client, localrobot
