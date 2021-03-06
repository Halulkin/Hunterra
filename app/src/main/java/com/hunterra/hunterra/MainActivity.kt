package com.hunterra.hunterra

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.hunterra.hunterra.databinding.ActivityMainBinding
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.*
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolDragListener
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyValue
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : AppCompatActivity(), OnMapReadyCallback, PermissionsListener,
    OnLocationClickListener, OnCameraTrackingChangedListener, MapboxMap.OnMapClickListener,
    MapboxMap.OnMapLongClickListener {

    var isOpen = false
    private var fab_open: Animation? = null
    private var fab_close: Animation? = null


    private val TAG = "MainActivity"

    private val MAKI_ICON_CAFE = "cafe-15"
    private val MAKI_ICON_HARBOR = "harbor-15"
    private val MAKI_ICON_AIRPORT = "airport-15"


    private val MARKER_IMAGE_ID = "MARKER_IMAGE_ID"


    private lateinit var binding: ActivityMainBinding

    private var permissionsManager: PermissionsManager? = null
    private var locationComponent: LocationComponent? = null
    private lateinit var mapboxMap: MapboxMap
    private var isInTrackingMode = false

    private lateinit var symbolManager: SymbolManager
    private var symbol: Symbol? = null

    // variables for calculating and drawing a route
    private var currentRoute: DirectionsRoute? = null
    private var navigationMapRoute: NavigationMapRoute? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, BuildConfig.MAPBOX_DOWNLOADS_TOKEN)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)


        fab_close = AnimationUtils.loadAnimation(applicationContext, R.anim.fab_close)
        fab_open = AnimationUtils.loadAnimation(applicationContext, R.anim.fab_open)
    }


    private fun fabClickListeners() {
        binding.fabMyLocation.setOnClickListener {
//            VolleySingleton.getInstance(this).volleyPost(this)
            moveToCurrentLocation()
        }

        binding.fabHunterra.setOnClickListener {
            moveToHunterraLocation()
        }

        binding.fabStartRoute.setOnClickListener {
            startNavigation()
        }

//        VolleySingleton.getInstance(this).volleyPost(this)

    }

    override fun onMapReady(mapboxMap: MapboxMap) {

        this.mapboxMap = mapboxMap

        mapboxMap.setStyle(
            Style.MAPBOX_STREETS
//            getString(R.string.navigation_guidance_day)
        ) { style ->
            enableLocationComponent(style)
            addDestinationIconSymbolLayer(style)
            initSymbolManager(style)
            setUpImage(style)

            mapboxMap.addOnMapClickListener(this)
            mapboxMap.addOnMapLongClickListener(this)

            fabClickListeners()


            //Just added for tast in Hunterra
            symbolManager.deleteAll()

            symbol = symbolManager.create(
                SymbolOptions()
                    .withLatLng(LatLng(50.087926, 14.418338))
                    .withIconImage(MARKER_IMAGE_ID)
                    .withIconSize(2.0f)
                    .withDraggable(false)
            )
        }
    }

    /* Setup possibility to use custom image in symbol manager icons */
    private fun setUpImage(loadedStyle: Style) {
        val drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_deer, null)
        val mBitmap = BitmapUtils.getBitmapFromDrawable(drawable)

        if (mBitmap != null) {
            loadedStyle.addImage(MARKER_IMAGE_ID, mBitmap)
        } else {
            Toast.makeText(this, "Image not loaded", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startNavigation() {
        val simulateRoute = true
        val options = NavigationLauncherOptions.builder()
            .directionsRoute(currentRoute)
            .shouldSimulateRoute(simulateRoute)
            .build()
        // Call this method with Context from within an Activity
        NavigationLauncher.startNavigation(this, options)
    }


    private fun initSymbolManager(style: Style) {
        // Set up a SymbolManager instance
        symbolManager = SymbolManager(binding.mapView, mapboxMap, style)
        symbolManager.iconAllowOverlap = true
        symbolManager.textAllowOverlap = true


        // add click listeners if desired
        symbolManager.addClickListener {
            showDialog()
        }

        symbolManager.addLongClickListener {

            Toast.makeText(
                this,
                getString(R.string.long_clicked_symbol_toast), Toast.LENGTH_SHORT
            ).show()
            symbol!!.iconImage = MAKI_ICON_AIRPORT
            symbolManager.update(symbol)
        }


        symbolManager.addDragListener(object : OnSymbolDragListener {
            // Left empty on purpose
            override fun onAnnotationDragStarted(annotation: Symbol) {}

            // Left empty on purpose
            override fun onAnnotationDrag(symbol: Symbol) {}

            // Left empty on purpose
            override fun onAnnotationDragFinished(annotation: Symbol) {}
        })
        Toast.makeText(
            this,
            getString(R.string.symbol_listener_instruction_toast), Toast.LENGTH_SHORT
        ).show()
    }


    private fun addDestinationIconSymbolLayer(loadedMapStyle: Style) {
        loadedMapStyle.addImage(
            "destination-icon-id",
            BitmapFactory.decodeResource(this.resources, R.drawable.mapbox_marker_icon_default)
        )
        val geoJsonSource = GeoJsonSource("destination-source-id")

        loadedMapStyle.addSource(geoJsonSource)

        val destinationSymbolLayer =
            SymbolLayer("destination-symbol-layer-id", "destination-source-id")

        destinationSymbolLayer.withProperties(
            PropertyFactory.iconImage("destination-icon-id"),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true),
            PropertyValue("Title", "Hello")
        )

        loadedMapStyle.addLayer(destinationSymbolLayer)
    }


    override fun onMapLongClick(point: LatLng): Boolean {

        val destinationPoint = Point.fromLngLat(point.longitude, point.latitude)
        val originPoint = Point.fromLngLat(
            locationComponent!!.lastKnownLocation!!.longitude,
            locationComponent!!.lastKnownLocation!!.latitude
        )
        val source = mapboxMap.style!!.getSourceAs<GeoJsonSource>("destination-source-id")
        source?.setGeoJson(Feature.fromGeometry(destinationPoint))

        getRoute(originPoint, destinationPoint)
        binding.fabStartRoute.isEnabled = true

        return true
    }

    private fun deletePoint() {

        val source = mapboxMap.style!!.getSourceAs<GeoJsonSource>("destination-source-id")

        if (source != null) {
            source.setGeoJson(FeatureCollection.fromFeatures(ArrayList()))
        } else {
            Toast.makeText(this, "Hadwdqwdqdqwdq", Toast.LENGTH_SHORT).show()
        }

//        val symbolLayerIconFeatureList: MutableList<Feature> = ArrayList()
//        symbolLayerIconFeatureList.add(
//            Feature.fromGeometry(
//                Point.fromLngLat(point.longitude, point.latitude)
//            )
//        )
//        symbolLayerIconFeatureList.add(
//            Feature.fromGeometry(
//                Point.fromLngLat(point.longitude, point.latitude)
//            )
//        )
    }


    override fun onMapClick(point: LatLng): Boolean {
        navigationMapRoute?.updateRouteVisibilityTo(false)
        binding.fabStartRoute.isEnabled = false

        deletePoint()

        if (isOpen) {
            closeFABMenu()
        }

        return true
    }


    /* Draw route from current location to destination point */
    private fun getRoute(origin: Point, destination: Point) {
        NavigationRoute.builder(this)
            .accessToken(Mapbox.getAccessToken()!!)
            .origin(origin)
            .destination(destination)
            .build()
            .getRoute(object : Callback<DirectionsResponse?> {
                @SuppressLint("LogNotTimber")
                override fun onResponse(
                    call: Call<DirectionsResponse?>,
                    response: Response<DirectionsResponse?>
                ) {
                    // You can get the generic HTTP info about the response
                    Log.d(TAG, "Response code: " + response.code())
                    if (response.body() == null) {
                        Log.e(
                            TAG,
                            "No routes found, make sure you set the right user and access token."
                        )
                        return
                    } else if (response.body()!!.routes().size < 1) {
                        Log.e(TAG, "No routes found")
                        return
                    }
                    currentRoute = response.body()!!.routes()[0]

                    /* Draw the route on the map */
                    if (navigationMapRoute != null) {
                        navigationMapRoute!!.updateRouteVisibilityTo(false)
                    } else {
                        navigationMapRoute = NavigationMapRoute(
                            null,
                            binding.mapView,
                            mapboxMap,
                            R.style.NavigationMapRoute
                        )
                    }
                    navigationMapRoute!!.addRoute(currentRoute)

                    /* Start animation of fabs to allow start navigation */

                    if (!isOpen) {
                        showFABMenu()
                    }
                }

                @SuppressLint("LogNotTimber")
                override fun onFailure(call: Call<DirectionsResponse?>, throwable: Throwable) {
                    Log.e(TAG, "Error: " + throwable.message)
                }
            })
    }

    /* Get current location by clicking on current point */
    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Enable the most basic pulsing styling by ONLY using the `.pulseEnabled()` method
            val customLocationComponentOptions = LocationComponentOptions.builder(this)
                .pulseEnabled(true)
                .build()

            // Get an instance of the component
            locationComponent = mapboxMap.locationComponent
            val locationComponentActivationOptions =
                LocationComponentActivationOptions.builder(this, loadedMapStyle)
                    .locationComponentOptions(customLocationComponentOptions)
                    .build()


            // Activate with options
            locationComponent!!.activateLocationComponent(locationComponentActivationOptions)

            // Enable to make component visible
            locationComponent!!.isLocationComponentEnabled = true

            // Set the component's camera mode
//            locationComponent!!.cameraMode = CameraMode.TRACKING

            // Set the component's render mode
            locationComponent!!.renderMode = RenderMode.COMPASS

            // Add the location icon click listener
            locationComponent!!.addOnLocationClickListener(this)

            // Add the camera tracking listener. Fires if the map camera is manually moved.
            locationComponent!!.addOnCameraTrackingChangedListener(this)

        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager!!.requestLocationPermissions(this)
        }
    }

    /*Animated move camera to current position*/
    private fun moveToCurrentLocation() {

        /*Animated*/
        val position = CameraPosition.Builder()
            .target(
                LatLng(
                    locationComponent!!.lastKnownLocation!!.latitude,
                    locationComponent!!.lastKnownLocation!!.longitude
                )
            )
            .zoom(17.0) // Sets the zoom
            .bearing(360.0) // Rotate the camera
            .tilt(30.0) // Set the camera tilt
            .build() // Creates a CameraPosition from the builder

        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 7000)

        /* Default animation */
        /*
            if (!isInTrackingMode) {
                isInTrackingMode = true
                locationComponent!!.cameraMode = CameraMode.TRACKING
                locationComponent!!.zoomWhileTracking(16.0)
                Toast.makeText(
                    this, getString(R.string.tracking_enabled),
                    Toast.LENGTH_SHORT
                ).show()

            } else {
                Toast.makeText(
                    this,
                    getString(R.string.tracking_already_enabled),
                    Toast.LENGTH_SHORT
                ).show()
            }
        */
    }

    /*Animated move camera to current position*/
    private fun moveToHunterraLocation() {
        symbolManager.deleteAll()

        symbol = symbolManager.create(
            SymbolOptions()
                .withLatLng(LatLng(50.087926, 14.418338))
                .withIconImage(MARKER_IMAGE_ID)
                .withIconSize(2.0f)
                .withDraggable(false)
        )

        symbolManager.update(symbol)

        val position = CameraPosition.Builder()
            .target(LatLng(50.087926, 14.418338))
            .zoom(17.0) // Sets the zoom
            .bearing(180.0) // Rotate the camera
            .tilt(30.0) // Set the camera tilt
            .build() // Creates a CameraPosition from the builder

        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 7000)
    }

    /* Get current location coordinates by clicking on current point */
    @SuppressLint("MissingPermission")
    override fun onLocationComponentClick() {
        if (locationComponent?.lastKnownLocation != null) {
            Toast.makeText(
                this, String.format(
                    getString(R.string.current_location),
                    locationComponent!!.lastKnownLocation?.latitude,
                    locationComponent!!.lastKnownLocation?.longitude
                ), Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCameraTrackingDismissed() {
        isInTrackingMode = false
    }

    override fun onCameraTrackingChanged(currentMode: Int) {
        // Empty on purpose
    }


    /* Animation part of Fabs */
    private fun showFABMenu() {
        isOpen = true
        binding.fabHunterra.animate().translationY(resources.getDimension(R.dimen.standard_63))
        binding.fabStartRoute.startAnimation(fab_open)
    }

    private fun closeFABMenu() {
        isOpen = false
        binding.fabStartRoute.animate().translationY(0F)
        binding.fabHunterra.animate().translationY(0F)
        binding.fabStartRoute.startAnimation(fab_close)
    }

    /* Permissions part of Mapbox */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        permissionsManager!!.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(this, "R.string.user_location_permission_explanation", Toast.LENGTH_LONG)
            .show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            mapboxMap.getStyle { style -> enableLocationComponent(style) }
        } else {
            Toast.makeText(this, "R.string.user_location_permission_not_granted", Toast.LENGTH_LONG)
                .show()
            finish()
        }
    }

    private fun showDialog() {

        val addPhotoBottomDialogFragment = AddPhotoBottomDialogFragment.newInstance()
        addPhotoBottomDialogFragment.show(
            supportFragmentManager,
            "add_photo_dialog_fragment"
        )

        val observable: Observable<Model> =
            VolleySingleton.getInstance(this).getObservable()


        val observer: Observer<Model> = object : Observer<Model> {
            override fun onSubscribe(d: Disposable) {}
            override fun onNext(model: Model) {
                addPhotoBottomDialogFragment.showImage(model)
            }

            override fun onError(e: Throwable) {
                Toast.makeText(
                    this@MainActivity,
                    "Problem with credentials, check entered data!",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onComplete() {
            }
        }

        observable.subscribe(observer)
    }

    /* Lifecycle methods for MapBox */
    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroy() {
        binding.mapView.onDestroy()
//        mapboxMap.removeOnMapClickListener(this)
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        binding.mapView.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

}
