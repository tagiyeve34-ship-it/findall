package com.ailenezareti.panelnext.ui
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ailenezareti.panelnext.databinding.ItemAlertBinding
import com.ailenezareti.panelnext.model.AlertEntry
class AlertAdapter(private val click:(AlertEntry)->Unit):RecyclerView.Adapter<AlertAdapter.H>(){
 var items:List<AlertEntry> = emptyList(); set(v){field=v;notifyDataSetChanged()};class H(val b:ItemAlertBinding):RecyclerView.ViewHolder(b.root)
 override fun onCreateViewHolder(p:ViewGroup,v:Int)=H(ItemAlertBinding.inflate(LayoutInflater.from(p.context),p,false));override fun getItemCount()=items.size
 override fun onBindViewHolder(h:H,i:Int){val x=items[i];h.b.message.text=x.message;h.b.date.text=x.created_at;if(x.is_read==0)h.b.message.alpha=1f else h.b.message.alpha=.65f;h.b.root.setOnClickListener{click(x)}}
}
