package com.homesoft.photo.google

import android.accounts.Account
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.os.AsyncTask
import android.util.Log
import androidx.annotation.WorkerThread
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.api.gax.core.CredentialsProvider
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import com.google.photos.library.v1.PhotosLibraryClient
import com.google.photos.library.v1.PhotosLibrarySettings
import java.io.IOException

const val RC_GET_AUTH_CODE = 1003
class MainActivity : AppCompatActivity() {
    lateinit var mGoogleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        signIn()
    }

    /**
     * Use the Web signIn.
     * We do this because it will bring up the Authorization View to access the scopes.
     * This only has to be done once, then you can use the AccountManager to get the Account directly.
     * Unfortunately, I don't currently know how to tell of an oauth scope is authorized.
     */
    private fun signIn() {
        //This is generated from google-services.json, for some reason Studio doesn't see it, but it compiles
        val webClientId = getString(R.string.default_web_client_id)
        val gso: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            /* We don't really care about the ServerAuthCode, but it's required to bring up the Authorization UI */
            .requestServerAuthCode(webClientId)
            .requestScopes(Scope(READONLY_SCOPE), Scope(SHARING_SCOPE))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_GET_AUTH_CODE)
    }

    @WorkerThread
    private fun getToken(account: Account) {
        val token:String
        try {
            //This will get an auth token using the Android signing config
            token = GoogleAuthUtil.getToken(this, account, "$OAUTH2$READONLY_SCOPE $SHARING_SCOPE")
        } catch (e:Exception) {
            Log.e(TAG, "getToken failed", e)
            return
        }
        test(getUserCredentials(token))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_GET_AUTH_CODE) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    /**
     * Now that we have creds, lest do a little test
     */
    @WorkerThread
    private fun test(credentialsProvider: CredentialsProvider) {
        Log.d(TAG, "test()")
        try {
            val photosLibraryClient = getPhotoLibraryClient(credentialsProvider)
            val pagedResponse = photosLibraryClient.listMediaItems()
            for ((i, mediaItem) in pagedResponse.iterateAll().withIndex()) {
                val name = mediaItem.filename
                Log.i(TAG, name)
                //Don't loop forever
                if (i >5) {
                    break;
                }
            }
        } catch (e:IOException) {
            Log.e(TAG, "test() failed", e)
        }
    }

    private fun getUserCredentials(token: String):CredentialsProvider {
        val a = AccessToken(token, null)
        val googleCredentials = GoogleCredentials.create(a)
        return FixedCredentialsProvider.create(googleCredentials)
    }

    private fun getPhotoLibraryClient(credentialsProvider: CredentialsProvider):PhotosLibraryClient {
        val settings: PhotosLibrarySettings = PhotosLibrarySettings
            .newBuilder()
            .setCredentialsProvider(credentialsProvider)
            .build()
        return PhotosLibraryClient.initialize(settings)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val googleSignInAccount = completedTask.getResult(ApiException::class.java) as GoogleSignInAccount
            val account = googleSignInAccount.account
            AsyncTask.SERIAL_EXECUTOR.execute {getToken(account)}
            // It is possible to get a Web auth token by using GoogleClientSecrets
            // https://developers.google.com/identity/sign-in/android/offline-access
            // But it's much more complicated
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
        }
    }

    companion object {
        const val TAG = "Photo"

        const val READONLY_SCOPE = "https://www.googleapis.com/auth/photoslibrary.readonly"
        const val SHARING_SCOPE = "https://www.googleapis.com/auth/photoslibrary.sharing"
        const val OAUTH2 = "oauth2:"
    }
}