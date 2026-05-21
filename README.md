# DropLAN - Local Network File Drop

Like AirDrop, but for Android and Linux devices on your local network.

## Features

- **File Transfer** - Send and receive files over LAN without internet
- **Clipboard Sync** - Sync clipboard content between devices
- **QR Code Pairing** - Quick device pairing via QR codes
- **Device Discovery** - Automatic local network device discovery
- **Transfer History** - Track all your transfers

## How It Works

1. Install DropLAN on two Android devices on the same WiFi network
2. Both devices discover each other automatically
3. Select a device, pick files, and send
4. Files transfer directly over TCP - no cloud, no accounts

## Tech Stack

- Kotlin 1.9.22 + Jetpack Compose
- Room Database for transfer history
- TCP/UDP for local network communication
- QR codes via ZXing

## Made By

**[jnetai.com](https://jnetai.com)**
