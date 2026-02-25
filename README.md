          TITLE : Peer-to-Peer Offline Secure Backup Recovery App Using Emoji-Based Authentication.

Problem Statement -  
          Many non-technical users lose their mobile data (photos, documents) due to device loss, damage, or reset.
          Most users are not aware of backup systems or depend fully on cloud & internet.

Purpose of This App - 
          To help non-technical users securely back up and recover important files offline, 
          without relying on cloud services, using emoji-based authentication.

Solution Overview -
This Android application allows users to:
   -Create a secure offline backup
   -Authenticate using emoji passwords (easy to remember, hard to guess)
   -Restore files without internet or cloud dependency

Key Features - 
  -Emoji-based password authentication
  -Works completely offline
  -Encrypted local backup storage
  -Supports images & documents
  -Brute-force protection (limited attempts)
  -Secure restore mechanism

Tech Stack -
  -Language: Java
  -Platform: Android Studio
  -Storage: Internal Storage
  -Security: SHA-256 + XOR Encryption
  -UI: XML

What I Learned -
  -Android file handling (URI, InputStream)
  -SharedPreferences for secure data storage
  -Basic encryption & hashing concepts
  -Authentication flow design
  -Secure backup & restore logic
