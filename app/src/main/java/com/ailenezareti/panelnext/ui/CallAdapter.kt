package com.ailenezareti.panelnext.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ailenezareti.panelnext.databinding.ItemCallBinding
import com.ailenezareti.panelnext.model.CallEntry
import com.ailenezareti.panelnext.util.Analytics

class CallAdapter:RecyclerView.Adapter<CallAdapter.H>(){
    var items:List<CallEntry> = emptyList(); set(v){field=v;notifyDataSetChanged()}
    class H(val b:ItemCallBinding):RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(p:ViewGroup,v:Int)=H(ItemCallBinding.inflate(LayoutInflater.from(p.context),p,false))
    override fun getItemCount()=items.size
    override fun onBindViewHolder(h:H,i:Int){ val x=items[i]; h.b.name.text=x.contact_name?.takeIf(String::isNotBlank)?:x.phone_number; h.b.meta.text="${Analytics.callType(x.call_type)} · ${x.occurred_at}"; h.b.duration.text=Analytics.minText(x.duration_sec) }
}
