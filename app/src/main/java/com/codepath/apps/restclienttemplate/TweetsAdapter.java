package com.codepath.apps.restclienttemplate;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.codepath.apps.restclienttemplate.models.Tweet;
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcels;

import java.util.List;

import okhttp3.Headers;

public class TweetsAdapter extends RecyclerView.Adapter<TweetsAdapter.ViewHolder> {

    private static final int REPLY_REQUEST_CODE = 30;
    Context context;
    List<Tweet> tweets;
    TwitterClient client;

    //Pass in the context and list of tweets


    public TweetsAdapter(Context context, List<Tweet> tweets, TwitterClient client) {
        this.context = context;
        this.tweets = tweets;
        this.client = client;
    }

    //For each row, inflate a layout
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tweet, parent, false);
        return new ViewHolder(view);
    }

    //Bind values based on the position of the element
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Tweet tweet = tweets.get(position);
        holder.bind(tweet, position);
    }

    @Override
    public int getItemCount() {
        return tweets.size();
    }

    // Clean all elements of the recycler
    public void clear() {
        tweets.clear();
        notifyDataSetChanged();
    }

    // Add a list of items -- change to type used
    public void addAll(List<Tweet> list) {
        tweets.addAll(list);
        notifyDataSetChanged();
    }




    //Define a viewholder

    public class ViewHolder extends RecyclerView.ViewHolder{

        ImageView ivProfileImage;
        TextView tvBody;
        TextView tvScreenName;
        TextView tvTimeStamp;
        TextView tvName;
        ImageView ivMediaImage;
        ImageView ivRetweeted;
        TextView tvRetweetedCount;
        ImageView ivFavorited;
        TextView tvFavoritedCount;
        ImageView ivReply;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfileImage = itemView.findViewById(R.id.ivProfileImage);
            tvBody = itemView.findViewById(R.id.tvBody);
            tvName = itemView.findViewById(R.id.tvName);
            tvScreenName = itemView.findViewById(R.id.tvScreenName);
            tvTimeStamp = itemView.findViewById(R.id.tvTimeStamp);
            ivMediaImage = itemView.findViewById(R.id.ivMediaImage);
            ivRetweeted = itemView.findViewById(R.id.ivRetweeted);
            tvRetweetedCount = itemView.findViewById(R.id.tvRetweetCount);
            ivFavorited = itemView.findViewById(R.id.ivFavorited);
            tvFavoritedCount = itemView.findViewById(R.id.tvFavoritedCount);
            ivReply = itemView.findViewById(R.id.ivReply);
        }

        public void bind(final Tweet tweet, final int position) {
            Log.i("retweet", "" + tweet.id + "tweet.user.name: " + tweet.user.name);
            Glide.with(context).load(tweet.user.profileImageUrl).into(ivProfileImage);
            tvBody.setText(tweet.body);
            tvName.setText(tweet.user.name);
            tvScreenName.setText("@" + tweet.user.screenName);
            tvTimeStamp.setText(tweet.getFormattedTimestamp());
            if(tweet.mediaUrl.equals("")){
                ivMediaImage.setVisibility(View.GONE);
            }else{
                ivMediaImage.setVisibility(View.VISIBLE);
                Glide.with(context).load(tweet.mediaUrl).into(ivMediaImage);
            }

            if(tweet.retweeted){
                ivRetweeted.setImageResource(R.drawable.ic_retweeted);
            }else{
                ivRetweeted.setImageResource(R.drawable.ic_defaultretweet);
            }

            tvRetweetedCount.setText(""+tweet.retweetCount);

            if(tweet.favorited){
                ivFavorited.setImageResource(R.drawable.ic_favorited);
            }else{
                ivFavorited.setImageResource(R.drawable.ic_defaultfavorite);
            }

            tvFavoritedCount.setText(""+tweet.favoriteCount);

            ivReply.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context, ComposeActivity.class);
                    intent.putExtra("composeOrReply", "reply");
                    intent.putExtra("tweet", Parcels.wrap(tweet));
                    ((Activity) context).startActivityForResult(intent, REPLY_REQUEST_CODE);
                }
            });

            ivRetweeted.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.i("tweet.retweed", "" + tweet.retweeted);
                    if(tweet.retweeted == false){
                        publishClientRetweet(tweet, client, position);
                    }else{
                        publishClientUnRetweet(tweet, client, position);
                    }

                }
            });

            ivFavorited.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.i("tweet.favorited", "" + tweet.favorited);
                    if(tweet.favorited == false){
                        publishClientTweetFavorited(tweet, client, position);
                    }else{
                        publishClientTweetUnFavorited(tweet, client, position);
                    }

                }
            });
            
            



        }

        private void publishClientTweetUnFavorited(Tweet tweet, TwitterClient client, final int position) {
            client.publishTweetUnFavorited(tweet.id, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Headers headers, JSON json) {
                    Log.i("unfavorited", "onSuccess");
                    Log.i("unfavorited", json.jsonObject.toString());

                    try {
                        //JSONObject retweet_status = json.jsonObject.getJSONObject("retweeted_status");
                        Tweet unfavoritedTweet = Tweet.fromJson(json.jsonObject);

                        tweets.set(position, unfavoritedTweet);
                        notifyDataSetChanged();

                        Log.i("unfavorited", "" + tweets.get(position).favorited);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                    Log.i("unfavorited", "onFailure" + response + throwable.toString());

                }
            });
        }

        private void publishClientTweetFavorited(Tweet tweet, TwitterClient client, final int position) {
            client.publishTweetFavorited(tweet.id, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Headers headers, JSON json) {
                    Log.i("favorited", "onSuccess");
                    Log.i("favorited", json.jsonObject.toString());

                    try {
                        //JSONObject retweet_status = json.jsonObject.getJSONObject("retweeted_status");
                        Tweet favoritedTweet = Tweet.fromJson(json.jsonObject);

                        tweets.set(position, favoritedTweet);
                        notifyDataSetChanged();

                        Log.i("favorited", "" + tweets.get(position).favorited);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                    Log.i("favorited", "onFailure" + response + throwable.toString());
                }
            });
        }

        private void publishClientUnRetweet(final Tweet tweet, TwitterClient client, final int position) {
            Log.i("unretweet", "unretweet");
            client.publishUnRetweet(tweet.id, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Headers headers, JSON json) {
                    Log.i("unretweet", "onSuccess");
                    Log.i("unretweet", json.jsonObject.toString());

                    try {

                        Tweet un_retweeted = Tweet.fromJson(json.jsonObject);
                        Log.i("unretweet", un_retweeted.toString());

                        //Manually set the retweet to false. Some kind of glitch
                        tweets.get(position).retweeted = false;
                        tweets.get(position).retweetCount--;
                        /*
                        Tried to set the un_retweeted tweet in the position but returns the original
                        tweet with retweeted as true and not false. Might have to do with
                        the following:

                        When passing a source status ID instead of the retweet status ID a HTTP 200
                        response will be returned with the same Tweet object but no action.
                        https://developer.twitter.com/en/docs/twitter-api/v1/tweets/post-and-engage/api-reference/post-statuses-unretweet-id
                         */
                        //tweets.set(position, un_retweeted );
                        notifyDataSetChanged();
                        Log.i("unretweet", "" + tweets.get(position).retweeted);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                    Log.i("unretweet", "onFailure" + response + throwable.toString());

                }
            });
        }

        private void publishClientRetweet(Tweet tweet, TwitterClient client, final int position) {
            Log.i("retweet", "retweet");

            Log.i("retweet", "" + tweet.id + "tweet.user.name: " + tweet.user.name);
            client.publishRetweet(tweet.id, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Headers headers, JSON json) {
                    Log.i("retweet", "onSuccess");
                    Log.i("retweet", json.jsonObject.toString());

                    try {
                        JSONObject retweet_status = json.jsonObject.getJSONObject("retweeted_status");
                        Tweet retweeted = Tweet.fromJson(retweet_status);

                        tweets.set(position, retweeted);
                        notifyDataSetChanged();
                        Log.i("retweet", "" + tweets.get(position).retweeted);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                    Log.i("retweet", "onFailure" + response + throwable.toString());

                }
            });
        }
    }
}
