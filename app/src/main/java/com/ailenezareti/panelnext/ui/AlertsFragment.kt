package com.ailenezareti.panelnext.ui
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ailenezareti.panelnext.api.ApiClient
import com.ailenezareti.panelnext.databinding.FragmentAlertsBinding
import com.ailenezareti.panelnext.model.*
import com.ailenezareti.panelnext.util.ChildResolver
import kotlinx.coroutines.launch
class AlertsFragment:Fragment(){private var _b:FragmentAlertsBinding?=null;private val b get()=_b!!;private lateinit var ad:AlertAdapter
 override fun onCreateView(i:LayoutInflater,c:ViewGroup?,s:Bundle?)=FragmentAlertsBinding.inflate(i,c,false).also{_b=it}.root
 override fun onViewCreated(v:View,s:Bundle?){ad=AlertAdapter{x->if(x.is_read==0)viewLifecycleOwner.lifecycleScope.launch{ApiClient.get(requireContext()).markRead(MarkReadRequest(x.id));load()}};b.list.layoutManager=LinearLayoutManager(requireContext());b.list.adapter=ad;b.swipe.setOnRefreshListener{load()};load()}
 private fun load(){viewLifecycleOwner.lifecycleScope.launch{b.swipe.isRefreshing=true;val id=ChildResolver.id(requireContext());try{ad.items=ApiClient.get(requireContext()).alerts(id).body()?.alerts.orEmpty().sortedByDescending{it.created_at}}catch(_:Exception){};b.swipe.isRefreshing=false}}
 override fun onDestroyView(){super.onDestroyView();_b=null}}
