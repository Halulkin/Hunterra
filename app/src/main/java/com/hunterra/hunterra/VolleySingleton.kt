package com.hunterra.hunterra

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import android.widget.Toast
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.json.JSONException
import org.json.JSONObject


class VolleySingleton constructor(context: Context) {

    private lateinit var result: String
    var response = PublishSubject.create<Model>()
    private var bearer = ""

    companion object {
        @Volatile
        private var INSTANCE: VolleySingleton? = null
        fun getInstance(context: Context) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: VolleySingleton(context).also {
                    INSTANCE = it
                }
            }
    }

    private val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(context.applicationContext)
    }

    fun <T> addToRequestQueue(req: Request<T>) {
        requestQueue.add(req)
    }


    fun getObservable(): Observable<Model> {
        return response
    }

    fun volleyLoginPost(login: String, mail: String, pass: String, context: Context) {

        if (checkCredentials(login, mail, pass)) {

            Log.e("*******", "$login $mail $pass")

            bearer = ""
            val postUrlLogin = "https://api.gamecam.cloud/api/user/login"

            val request: StringRequest =
                @SuppressLint("LogNotTimber")
                object : StringRequest(Method.POST, postUrlLogin,
                    Response.Listener { response ->
                        if (response != null) {
                            val responseObj = JSONObject(response)
                            val token = responseObj.getString("token")
                            Log.e("Volley", "token : $token")
                            bearer = token

                            if (bearer != "") {
                                volleyGetImage()
                            }

                        } else {
                            Log.e("Your Array Response", "Data Null")
                        }
                    },
                    Response.ErrorListener { error ->
                        Log.e("error is ", "" + error)
                    }) {

                    //This is for Headers If You Needed
                    @Throws(AuthFailureError::class)
                    override fun getHeaders(): Map<String, String> {
                        val params: MutableMap<String, String> = HashMap()
                        params["apiKey"] = "u80jpfen61r75uxmg2zctlslj57mdqxu"
                        return params
                    }

                    //Pass Your Parameters here
                    override fun getParams(): Map<String, String> {
                        val params: MutableMap<String, String> = HashMap()

                        // inside of any of your application's code
                        val userToken: String = BuildConfig.SECRET_USER_TOKEN
                        params["email"] = "test@hunterra.eu"
                        params["token"] = userToken

                        return params
                    }
                }
//        val queue = Volley.newRequestQueue(context)
//        queue.add(request)

            requestQueue.add(request)
        } else {
            Toast.makeText(
                context,
                "Problem with credentials, check entered data!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun volleyGetImage() {
        val model = Model()
        val getUrlImage = "https://api.gamecam.cloud/api/image/user/89ip8id6gf0rz9la"

        val request: StringRequest =
            @SuppressLint("LogNotTimber")
            object : StringRequest(Method.GET, getUrlImage,
                Response.Listener { response ->
                    if (response != null) {
                        val responseObj = JSONObject(response)

                        if (responseObj.has("data")) {

                            val jsonArray = responseObj.getJSONArray("data")
                            val jsonObject = jsonArray.getJSONObject(0)
                            val image = jsonObject.getString("imgUrl")

                            Log.e("Volley", "image : $image")

                            model.image = image

                            this.response.onNext(model)
                        }

                    } else {
                        Log.e("Your Array Response", "Data Null")
                    }
                },
                Response.ErrorListener { error -> Log.e("error is ", "" + error) }) {

                //This is for Headers If You Needed
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val params: MutableMap<String, String> = HashMap()
                    params["apiKey"] = "u80jpfen61r75uxmg2zctlslj57mdqxu"
                    params["Authorization"] = "Bearer $bearer"

                    return params
                }
            }
//        val queue = Volley.newRequestQueue(context)
//        queue.add(request)
        requestQueue.add(request)
    }

    private fun checkCredentials(login: String, mail: String, pass: String): Boolean {
        return login == "hunterra_test" && mail == "test@hunterra.eu" && pass == "4X_egYMMde"
    }

    @SuppressLint("LogNotTimber")
    fun volleyGet(context: Context) {

        val email = "email"
        val model = Model()

        val url = "https://reqres.in/api/users?page=2"
//        val url = "https://api.gamecam.cloud/api/cam?"


        val jsonResponses: MutableList<String> = ArrayList()
        val requestQueue = Volley.newRequestQueue(context)
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val jsonArray = response.getJSONArray("data")
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        model.login = jsonObject.getString("email")
                        model.image = jsonObject.getString("avatar")
                        jsonResponses.add(email)

                        this.response.onNext(model)

                        Log.e("Volley", "jsonParse:$email")

                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }) { error -> error.printStackTrace() }
        requestQueue.add(jsonObjectRequest)
    }

    @SuppressLint("LogNotTimber")
    fun volleyPost(context: Context) {
//        val postUrl = "https://reqres.in/api/users"
        val postUrl = "https://api.gamecam.cloud/api/user/login"


        val requestQueue = Volley.newRequestQueue(context)
        val postData = JSONObject()
        try {
            postData.put("email", "test@hunterra.eu")
            postData.put("password", "4X_egYMMde")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST,
            postUrl,
            postData,
            { response ->
                Log.e("Volley", response.toString())
            }
        ) { error -> error.printStackTrace() }
        requestQueue.add(jsonObjectRequest)
        Log.e("Volley", jsonObjectRequest.toString())
    }

    @SuppressLint("LogNotTimber")
    private fun jsonParse() {
        val url = "https://api.myjson.com/bins/xbspb"
        val request =
            JsonObjectRequest(Request.Method.GET, url, null, { response ->
                try {
                    val jsonArray = response.getJSONArray("employees")
                    for (i in 0 until jsonArray.length()) {
                        val employee = jsonArray.getJSONObject(i)
                        val firstName = employee.getString("firstname")
                        val age = employee.getInt("age")
                        val mail = employee.getString("mail")

                        result = "$firstName, $age, $mail\n\n"

                        Log.e("Volley", "jsonParse:$result")

                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }, { error -> error.printStackTrace() })
        requestQueue.add(request)
    }

    val imageLoader: ImageLoader by lazy {
        ImageLoader(requestQueue,
            object : ImageLoader.ImageCache {
                private val cache = LruCache<String, Bitmap>(20)
                override fun getBitmap(url: String): Bitmap {
                    return cache.get(url)
                }

                override fun putBitmap(url: String, bitmap: Bitmap) {
                    cache.put(url, bitmap)
                }
            })
    }

}