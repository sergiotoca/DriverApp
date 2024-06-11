package com.groupf.driverapp.ui.home

import android.content.res.Resources
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.driverapp.R
import com.example.driverapp.databinding.FragmentHomeBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import java.util.jar.Manifest

class HomeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var homeViewModel: HomeViewModel

    private lateinit var mapFragment: SupportMapFragment

    //Location
    private lateinit var locationRequest:com.google.android.gms.location.LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var _binding: FragmentHomeBinding? = null


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        init()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        return root
    }

    private fun init() {
        locationRequest = com.google.android.gms.location.LocationRequest()
        locationRequest.setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationRequest.setFastestInterval(3000)
        locationRequest.interval = 5000
        locationRequest.setSmallestDisplacement(10f)

        locationCallback = object: LocationCallback() {

            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)

                val newPos = LatLng(locationResult!!.lastLocation.latitude,locationResult!!.lastLocation.longitude )
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos,18f))
            }
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context!!)
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper())
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        //Request permission
        Dexter.withContext(requireContext())
            .withPermissions(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        Log.d("Permissions", "All permissions are granted.")
                        //Enable Button
                        mMap.isMyLocationEnabled = true
                        mMap.uiSettings.isMyLocationButtonEnabled = true
                        mMap.setOnMyLocationClickListener {
                            fusedLocationProviderClient.lastLocation
                                .addOnFailureListener { e ->
                                    Toast.makeText(context!!, e.message, Toast.LENGTH_SHORT).show()
                                }.addOnSuccessListener { location ->
                                    if (location != null) {
                                        val userLatLng = LatLng(location.latitude, location.longitude)
                                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 18f))
                                    }
                                }
                            true
                        }

                        // Layout - Correctly handle the location button
                        val mapView = mapFragment.requireView()
                        val locationButton: View = (mapView.findViewById<View>(Integer.parseInt("1"))!!.parent as View).findViewById(Integer.parseInt("2"))

                        if (locationButton is ImageView) {
                            // You might not need to adjust the layout params if the view is ImageView
                            Log.d("Permissions", "Found the location button as ImageView.")
                        } else if (locationButton is ViewGroup) {
                            Log.d("Permissions", "Found the location button as ViewGroup.")
                            val params = locationButton.layoutParams as RelativeLayout.LayoutParams
                            params.addRule(RelativeLayout.ALIGN_TOP, 0)
                            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                            params.bottomMargin = 50
                            locationButton.layoutParams = params
                        } else {
                            Log.d("Permissions", "Location button is of an unexpected type: ${locationButton.javaClass.simpleName}")
                        }
                    } else {
                        Log.d("Permissions", "Not all permissions are granted.")
                    }

                    if (report.isAnyPermissionPermanentlyDenied) {
                        // Handle the case when permission is permanently denied
                        Log.d("Permissions", "Permission permanently denied.")
                        Toast.makeText(context!!, "Permission was permanently denied!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(permissions: List<PermissionRequest>, token: PermissionToken) {
                    Log.d("Permissions", "Showing permission rationale.")
                    token.continuePermissionRequest()
                }
            }).check()




        try {
            val success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(),R.raw.uber_maps_style))
            if(!success)
                Log.e("MAP_ERROR", "Style parsing error")
        }catch (e:Resources.NotFoundException)
        {
            Log.e("MAP_ERROR", e.message.toString())
        }


    }


}