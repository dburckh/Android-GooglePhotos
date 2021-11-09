# Android-GooglePhotos
A stub app showing how to access Google Photos (or any other Google API) from Android.  This app uses your apps signature instead of a secret to authenticate.  It requires more setup, but it's considerably less code.  I believe it's more secure as well.

##  Setup
- You'll need find your keystore.debug.  You can Google to find it's location for your OS.  For me it was in ~/.android/debug.keystore.  I like to copy it into a directly parallel to may app do I don't check it in.
- Setup your app in Firebase.
  - https://console.firebase.google.com/
- You'll need to enable the Photos API
  - https://developers.google.com/photos/library/guides/get-started-java
- You'll need to create "OAuth 2.0 Client IDs" for your app's debug and, eventually, it's release signature to authenticate without a secret.
  - Head to https://console.cloud.google.com/apis/credentials
  - Hit "+CREATE CREDENTIALS"
  - Follow the prompts
- While you are in the Cloud Console, setup the OAuth consent screen.
  - This is where you set your OAuth scopes (see Photos API for a complete list).  I would start with read only.
  - Since this is a test app, you'll need to add some users.
- Go into Firebase and generate your google-services.json.  Put it in the "app" root as described A little Google magic here.  Your OAuth 2.0 Client ID gets added!
  - https://support.google.com/firebase/answer/7015592
  - Make sure you add a support email while you are at it, some people have had issues if you don't.

### Refresh Tokens
Like all OAuth tokens, the token returned from GoogleAuthUtil.getToken() will eventually need to be refreshed.  Since I'm using a GoogleCredentials class it may happen automatically, but most likely you'll have to call GoogleAuthUtil.getToken() again.

