package com.codepath.apps.restclienttemplate;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.codepath.apps.restclienttemplate.models.Tweet;
import com.codepath.apps.restclienttemplate.models.User;
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler;
import com.google.gson.Gson;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.parceler.Parcels;

import okhttp3.Headers;

public class ComposeActivity extends AppCompatActivity {

    public static final String TAG ="ComposeActivity";
    public static final int MAX_TWEET_LENGTH = 280;
    EditText etCompose;
    Button btnTweet;
    TwitterClient client;
    TextView tvCharLimit;
    User loginUser;
    ImageView ivProfileImage;
    TextView tvName;
    TextView tvScreenName;

    LinearLayout llReplyLayout;
    Button buttonCancelTweet;

    //Bundle savedInstanceState;

    SharedPreferences sharedPref;
    TextView tvInReplyTo;
    Tweet inReplyTweet;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        client = TwitterApp.getRestClient(this);

        etCompose = findViewById(R.id.etCompose);
        btnTweet = findViewById(R.id.btnTweet);
        tvCharLimit = findViewById(R.id.tvCharLimit);
        ivProfileImage = findViewById(R.id.ivProfileImage);
        tvName = findViewById(R.id.tvName);
        tvScreenName = findViewById(R.id.tvScreenName);
        buttonCancelTweet = findViewById(R.id.buttonCancelTweet);

        llReplyLayout = findViewById(R.id.llReplyLayout);
        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        tvInReplyTo = findViewById(R.id.tvInReplyTo);



        //this.savedInstanceState = savedInstanceState;


        final String composeOrReply= getIntent().getStringExtra("composeOrReply");
        if(composeOrReply.equals("compose")){
            llReplyLayout.setVisibility(View.GONE);
            Gson gson = new Gson();
            String loginUserJson = sharedPref.getString("MyLoginUser","");
            Log.i(TAG,"loginUserJson " + loginUserJson);
            if(loginUserJson.equals("")){
                getUserInfo();
            }else{
                loginUser = gson.fromJson(loginUserJson,User.class);
                populateProfile();
            }


   
        }else if(composeOrReply.equals("reply")){

            Log.i(TAG, "REPLY");
            inReplyTweet = Parcels.unwrap(getIntent().getParcelableExtra("tweet"));
            Log.i("tweet", inReplyTweet.user.name);

            llReplyLayout.setVisibility(View.VISIBLE);
            populateReplyTweet(inReplyTweet.user);



        }


        etCompose.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Fires right as the text is being changed (even supplies the range of text)

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Fires right before text is changing

            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Fires right after the text has changed

                int tweetCharLeft = MAX_TWEET_LENGTH - editable.toString().length();

                tvCharLimit.setText("" + tweetCharLeft);
                if(tweetCharLeft<0){
                    btnTweet.setEnabled(false);
                    //tvCharLimit.setTextColor(Color.parseColor("#FF0000"));
                }else{
                    btnTweet.setEnabled(true);
                    //tvCharLimit.setTextColor(Color.parseColor("#000000"));
                }
            }
        });

        buttonCancelTweet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("cancel", "cancel");
                String composeTweet = etCompose.getText().toString();
                if(composeTweet.length() > 0){
                    AlertDialog.Builder builder = new AlertDialog.Builder(ComposeActivity.this);
                    builder.setMessage("Cancel the message?");

                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                            finish();
                        }
                    });

                    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    });

                    AlertDialog cancelAlert = builder.create();
                    cancelAlert.show();
                }else{
                    finish();
                }
            }
        });

        //Set click listener on button
        btnTweet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //
                String tweetContent = etCompose.getText().toString();
                if(tweetContent.isEmpty()){
                    Toast.makeText(ComposeActivity.this, "Sorry, your tweet cannot be empty", Toast.LENGTH_LONG).show();
                }

                if(tweetContent.length() > MAX_TWEET_LENGTH){
                    Toast.makeText(ComposeActivity.this, "Sorry, your tweet is too long", Toast.LENGTH_LONG).show();
                }

                Toast.makeText(ComposeActivity.this,tweetContent, Toast.LENGTH_LONG).show();

                if(composeOrReply.equals("compose")) {
                    Log.i(TAG, "COMPOSE");
                    //Make an API call to Twitter to publish the tweet
                    publishClientTweet(tweetContent);

                }else if(composeOrReply.equals("reply")){
                    publishClientReply(tweetContent);

                }
            }
        });
    }

    private void publishClientReply(String tweetContent) {
        Log.i(TAG, "rePLY");
        client.publishReply(inReplyTweet.id, tweetContent, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {
                try {
                    Tweet tweet = Tweet.fromJson(json.jsonObject);
                    Log.i(TAG, "Reply tweet says: " + tweet.body);
                    Intent intent = new Intent();
                    intent.putExtra("tweet", Parcels.wrap(tweet));
                    setResult(RESULT_OK, intent); // set result code and bundle data for response
                    finish(); // closes the activity, pass data to parent
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                    Log.e(TAG, "ONfAILURE: " + response);
            }
        });
    }

    private void publishClientTweet(String tweetContent) {
        client.publishTweet(tweetContent, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {
                Log.i(TAG, "onSuccess to publish tweet");
                try {
                    Tweet tweet = Tweet.fromJson(json.jsonObject);
                    Log.i(TAG, "Published tweet says: " + tweet.body);
                    Intent intent = new Intent();
                    intent.putExtra("tweet", Parcels.wrap(tweet));
                    setResult(RESULT_OK, intent); // set result code and bundle data for response
                    finish(); // closes the activity, pass data to parent
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                Log.e(TAG, "onFailure to publish tweet", throwable);
            }
        });
    }

    private void populateReplyTweet(User inReplyTweetUser) {
        Glide.with(this).load(inReplyTweetUser.profileImageUrl).into(ivProfileImage);
        tvName.setText(inReplyTweetUser.name);
        tvScreenName.setText("@" + inReplyTweetUser.screenName);

        etCompose.setText("@" + inReplyTweetUser.screenName + " ");
        tvInReplyTo.setText("In reply to " + inReplyTweetUser.screenName);
        int charLimit = MAX_TWEET_LENGTH - inReplyTweetUser.screenName.length();
        tvCharLimit.setText("" + charLimit);

    }

    private void populateProfile() {
        Glide.with(this).load(loginUser.profileImageUrl).into(ivProfileImage);
        tvName.setText(loginUser.name);
        tvScreenName.setText("@" + loginUser.screenName);
        tvCharLimit.setText(""+MAX_TWEET_LENGTH);
    }

    private void getUserInfo() {
        client.accountCredentials(new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {
                try {
                    loginUser = User.fromJson(json.jsonObject);
                    Log.i(TAG, "Login User Name: " + loginUser.name);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    Gson gson = new Gson();
                    String loginUserJson = gson.toJson(loginUser);
                    editor.putString("MyLoginUser", loginUserJson);
                    editor.commit();

                    populateProfile();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                Log.e(TAG, "onFailure Response " + response);

            }
        });
    }


}