package com.example.mosis_new

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.icu.text.SimpleDateFormat
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.mosis_new.databinding.FragmentMapBinding
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import coil.load
import com.example.mosis_new.model.FilterObject
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.infowindow.InfoWindow.closeAllInfoWindowsOn
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.Locale

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val user: com.example.mosis_new.model.User by activityViewModels()
    private val filterObject: FilterObject by activityViewModels()
    lateinit var map: MapView
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var location: Location? = null
    private var markers: List<MarkerObjecats?>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Firebase.firestore
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentMapBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx: Context? = activity?.applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx!!))
        map = requireView().findViewById<MapView>(R.id.map)
        map.setMultiTouchControls(true)

        if(ActivityCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    setMyLocationOverlay()
                    LocationServices.getFusedLocationProviderClient(requireContext()).lastLocation.addOnSuccessListener {
                        location = it
                    }
                }
            }.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }else {
            setMyLocationOverlay()
            LocationServices.getFusedLocationProviderClient(requireContext()).lastLocation.addOnSuccessListener {
                location = it
            }
        }
        map.controller.setZoom(15.0)
        map.controller.setCenter(GeoPoint(43.3209, 21.8958))

        val rotationOverlay = RotationGestureOverlay(map)
        rotationOverlay.isEnabled = true
        map.overlays.add(rotationOverlay)
        val compassOverlay = object: CompassOverlay(context, map){
            override fun draw(c: Canvas?, pProjection: Projection?) {
                drawCompass(c, -map.mapOrientation, pProjection?.screenRect)
            }

            override fun onSingleTapUp(e: MotionEvent?, mapView: MapView?): Boolean {
                map.setMapOrientation(0.0f,false)
                return true
            }
        }
        val scaleBar = ScaleBarOverlay(map)
        map.overlays.add(scaleBar)
        compassOverlay.enableCompass()
        map.overlays.add(compassOverlay)
//        var marker = Marker(map)
////        marker.
//        map.overlays.add(marker)
//        Location.distanceBetween()


        placeMarkersOnTheMap()
        initializeTexViews()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    // My functions

    private fun initializeTexViews(){
        val navView: NavigationView = requireActivity().findViewById<NavigationView>(R.id.nav_view)

        navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.signOut -> {
                    Firebase.auth.signOut()
                    user.setFirebaseUser(Firebase.auth.currentUser)
                    findNavController().navigate(R.id.loginFragment)
                }
                R.id.rangs -> {
                    findNavController().navigate(R.id.rangsFragment)
                }
            }
            true
        }

        val name: TextView = navView.getHeaderView(0).findViewById<TextView>(R.id.nav_header_name)
        val lastName: TextView = navView.getHeaderView(0).findViewById<TextView>(R.id.nav_header_lastName)
        val photo: ShapeableImageView = navView.getHeaderView(0).findViewById<ShapeableImageView>(R.id.menu_header_image)

        user.displayName.observe(viewLifecycleOwner){
            name.text = it
        }
        user.email.observe(viewLifecycleOwner){
            lastName.text = it
        }
        user.photoUri.observe(viewLifecycleOwner){
            photo.load(it)
        }

//        var id: String? = auth.currentUser!!.email

//        val docRef = db.collection("users").document(id!!)
//
//        docRef.get()
//            .addOnSuccessListener { document ->
//                if(document != null){
//                    var user = document.toObject(User::class.java)
//                    name.text = user!!.name
//                    lastName.text = user.lastName
//                }
//            }

    }
    private fun setMyLocationOverlay(){
        val myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(activity),map)
        myLocationOverlay.enableMyLocation()
        map.overlays.add(myLocationOverlay)
    }

    private fun filter(){
        closeAllInfoWindowsOn(map)
        if(markers != null){
            var autorValid : Boolean = true
            var descValid : Boolean = true
            var kalendarValid : Boolean = true
            var radiusValid : Boolean = true

            for(marker in markers!!){
                if(marker != null){
//                    val date = (marker.doc!!.data!!["date"] as Timestamp).toDate()

//                    (marker.doc!!.data!!["user"] as DocumentReference).get().addOnSuccessListener{
//                        if(filterObject.autor.value != null){
//                            Log.i("Autor", "marker.doc!!.data!![\"user\"] ${(marker.doc!!.data!!["user"] as DocumentReference).id}")
//                            Log.i("Autor", "it.data!![\"name\"] ${(it.data!! as DocumentReference).id}")
//                            autorValid = (it.data!! as DocumentReference).id.contains(filterObject.autor.value!!)
//                            Log.i("Autor", "Je ${autorValid}")
//                        }else{
//                            Log.i("Autor", "Je Null")
//                        }

                        if(filterObject.autor.value != null){
                            Log.i("Autor", "marker.doc!!.data!![\"user\"] ${(marker.doc!!.data!!["user"] as DocumentReference).id}")
                            Log.i("Autor", "it.data!![\"name\"] ${(marker.doc!!.data!!["user"] as DocumentReference).id}")
                            autorValid = (marker.doc!!.data!!["user"] as DocumentReference).id.contains(filterObject.autor.value!!)
                            Log.i("Autor", "Je ${autorValid}")
                        }else {
                            autorValid = true
                        }

                    val desc : String = marker.doc!!.data!!["destination"] as String

//                    if(filterObject.startDate.value != null)
//                        if(date.(filterObject.startDate.value)){
//                            kalendarValid = true
//                            Toast.makeText(activity, "${filterObject.startDate.value}", Toast.LENGTH_SHORT).show()
//                        }else{
//                            Toast.makeText(activity, "Dan pre", Toast.LENGTH_SHORT).show()
//                            Toast.makeText(activity, "${filterObject.startDate.value}", Toast.LENGTH_SHORT).show()
//                        }
//                    else
//                        Toast.makeText(activity, "Null", Toast.LENGTH_SHORT).show()

                    if(location!=null && filterObject.radius.value != null){
                        val dist = FloatArray(1)
                        Location.distanceBetween(
                            location!!.latitude,
                            location!!.longitude,
                            marker.position.latitude,
                            marker.position.longitude,
                            dist
                        )
                        if(filterObject.radius.value.isNullOrEmpty()){
                            radiusValid = true
                        }else{
                            radiusValid = dist[0]/1000 <= filterObject.radius.value!!.toInt()
                        }
                    }


                    if(!filterObject.desc.value.isNullOrEmpty()){
                        descValid = desc.contains(filterObject.desc.value!!)
                        Log.i("Destination", "desc $desc")
                        Log.i("Destination", "filterObject.desc.value ${filterObject.desc.value}")
                    }else{
                        Log.i("Destination", "je null")
                        descValid = true
                    }
                    marker.isEnabled = autorValid  && radiusValid && descValid
                }
            }

        }
    }

    private fun placeMarkersOnTheMap(){

        db.collection("destinations").get().addOnSuccessListener {
            markers = List<MarkerObjecats?>(it.size()) { i ->
                val document = it.documents[i]
                if (document == null || document.data!!["location"] == null) null
                else {
                    val marker = MarkerObjecats(map, document)
                    val loc = document.data!!["location"] as com.google.firebase.firestore.GeoPoint
                    marker.position = GeoPoint(loc.latitude, loc.longitude)

                    marker.infoWindow = object : MarkerInfoWindow(R.layout.bubble, map) {
                        var dovoljno = true

                        override fun onOpen(item: Any?) {
                            closeAllInfoWindowsOn(map)
                            super.onOpen(item)
                            if(dovoljno){
                                dovoljno = false

                                marker.infoWindow?.view?.findViewById<ImageView>(R.id.bubble_mainImage)
                                    ?.load(Uri.parse(document.data!!["imageUri"] as String))

                                marker.infoWindow?.view?.findViewById<TextView>(R.id.bubble_date)?.text =
                                    SimpleDateFormat("dd. MM. yyyy.", Locale.getDefault()).format((document.data!!["date"] as Timestamp).toDate())
                            }
                            marker.infoWindow?.view?.findViewById<TextView>(R.id.bubble_title)?.text = document.data!!["destination"] as String
                            marker.infoWindow?.view?.findViewById<TextView>(R.id.bubble_description)?.text = document.data!!["description"] as String
                            (document.data!!["user"] as DocumentReference).get().addOnSuccessListener {
                                marker.infoWindow?.view?.findViewById<ImageView>(R.id.bubble_authorImage)
                                    ?.load(Uri.parse(it.data!!["photoUri"] as String))
                                marker.infoWindow?.view?.findViewById<TextView>(R.id.bubble_author)?.text = it.data!!["name"] as String
                            }

                            var distance : Float
                            distance = 100F
                            if(location != null){
                                val dist = FloatArray(1)
                                Location.distanceBetween(
                                    location!!.latitude,
                                    location!!.longitude,
                                    marker.position.latitude,
                                    marker.position.longitude,
                                    dist
                                )
                                distance = dist[0]
                            }

                            var list: ArrayList<String> = document.data!!["like"] as ArrayList<String>
                            if(list.contains(user.email.value.toString())){
                                marker.infoWindow?.view?.findViewById<TextView>(R.id.bubble_favorite_red)?.visibility = View.VISIBLE
                                marker.infoWindow?.view?.findViewById<TextView>(R.id.bubble_favorite_white)?.visibility = View.GONE
                            }else{
                                marker.infoWindow?.view?.findViewById<TextView>(R.id.bubble_favorite_red)?.visibility = View.GONE
                                marker.infoWindow?.view?.findViewById<TextView>(R.id.bubble_favorite_white)?.visibility = View.VISIBLE
                            }
                            marker.infoWindow?.view?.findViewById<TextView>(R.id.bubble_favorite_white)?.setOnClickListener{
                                if(distance < 100){
                                    var array: ArrayList<String> = document.data!!["like"] as ArrayList<String>
                                    db.collection("users").document(user.email.value.toString())
                                        .update("score", FieldValue.increment(1))
                                    array.add(user.email.value.toString())
                                    db.collection("destinations").document(document.id)
                                        .update("like", array)
                                    marker.infoWindow?.view?.findViewById<TextView>(R.id.bubble_favorite_red)?.visibility = View.VISIBLE
                                    marker.infoWindow?.view?.findViewById<TextView>(R.id.bubble_favorite_white)?.visibility = View.GONE
                                }else{
                                    Toast.makeText(activity, "You have to be closer than 100m of location!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            marker.infoWindow?.view?.findViewById<TextView>(R.id.bubble_favorite_red)?.setOnClickListener{
                                if(distance < 100){
                                var array: ArrayList<String> = document.data!!["like"] as ArrayList<String>
                                array.remove(user.email.value.toString())
                                db.collection("users").document(user.email.value.toString())
                                    .update("score", FieldValue.increment(-1))
                                db.collection("destinations").document(document.id)
                                    .update("like", array)
                                marker.infoWindow?.view?.findViewById<TextView>(R.id.bubble_favorite_red)?.visibility = View.GONE
                                marker.infoWindow?.view?.findViewById<TextView>(R.id.bubble_favorite_white)?.visibility = View.VISIBLE}else{
                                    Toast.makeText(activity, "You have to/ be closer than 100m of location!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    map.overlays.add(marker)
                    marker
                }
            }
            filter()
        }
    }
}