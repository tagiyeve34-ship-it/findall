package com.ailenezareti.panelnext.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ailenezareti.panelnext.api.ApiClient
import com.ailenezareti.panelnext.databinding.FragmentCallsBinding
import com.ailenezareti.panelnext.model.CallEntry
import com.ailenezareti.panelnext.util.Analytics
import com.ailenezareti.panelnext.util.ChildResolver
import kotlinx.coroutines.launch

class CallsFragment:Fragment(){
    private var _b:FragmentCallsBinding?=null;private val b get()=_b!!;private val ad=CallAdapter();private var all:List<CallEntry> = emptyList();private var filter="all"
    override fun onCreateView(i:LayoutInflater,c:ViewGroup?,s:Bundle?)=FragmentCallsBinding.inflate(i,c,false).also{_b=it}.root
    override fun onViewCreated(v:View,s:Bundle?){b.list.layoutManager=LinearLayoutManager(requireContext());b.list.adapter=ad;b.swipe.setOnRefreshListener{load()};b.allBtn.setOnClickListener{filter="all";apply()};b.inBtn.setOnClickListener{filter="Gələn";apply()};b.outBtn.setOnClickListener{filter="Gedən";apply()};b.missedBtn.setOnClickListener{filter="Cavabsız";apply()};b.search.addTextChangedListener(object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,st:Int,c:Int,a:Int){};override fun onTextChanged(s:CharSequence?,st:Int,bf:Int,c:Int)=apply();override fun afterTextChanged(e:Editable?){}});load()}
    private fun load(){viewLifecycleOwner.lifecycleScope.launch{b.swipe.isRefreshing=true;val id=ChildResolver.id(requireContext());try{all=ApiClient.get(requireContext()).calls(id,limit=500).body()?.calls.orEmpty()}catch(_:Exception){};apply();b.swipe.isRefreshing=false}}
    private fun apply(){val q=b.search.text.toString().trim().lowercase();val x=all.filter{(filter=="all"||Analytics.callType(it.call_type)==filter)&&(q.isBlank()||it.phone_number.contains(q)||(it.contact_name?.lowercase()?.contains(q)==true))}.sortedByDescending{Analytics.date(it.occurred_at)?.time?:0};ad.items=x;b.summary.text="${x.size} nəticə · ümumi danışıq ${Analytics.minText(x.sumOf{it.duration_sec})}"}
    override fun onDestroyView(){super.onDestroyView();_b=null}
}
