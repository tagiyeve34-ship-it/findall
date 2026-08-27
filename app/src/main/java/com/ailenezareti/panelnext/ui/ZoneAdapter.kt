package com.ailenezareti.panelnext.ui
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ailenezareti.panelnext.databinding.ItemZoneBinding
import com.ailenezareti.panelnext.model.GeoZone
class ZoneAdapter(private val del:(GeoZone)->Unit):RecyclerView.Adapter<ZoneAdapter.H>(){
 var items:List<GeoZone> = emptyList(); set(v){field=v;notifyDataSetChanged()}
 class H(val b:ItemZoneBinding):RecyclerView.ViewHolder(b.root)
 override fun onCreateViewHolder(p:ViewGroup,v:Int)=H(ItemZoneBinding.inflate(LayoutInflater.from(p.context),p,false))
 override fun getItemCount()=items.size
 override fun onBindViewHolder(h:H,i:Int){val x=items[i];h.b.name.text=x.name;h.b.meta.text="${x.radius_m} m radius · ${if(x.notify_enter==1) "Giriş " else ""}${if(x.notify_exit==1) "Çıxış" else ""}";h.b.delete.setOnClickListener{del(x)}}
}
