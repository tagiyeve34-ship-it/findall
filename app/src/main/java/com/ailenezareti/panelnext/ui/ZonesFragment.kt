package com.ailenezareti.panelnext.ui

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ailenezareti.panelnext.api.ApiClient
import com.ailenezareti.panelnext.databinding.FragmentZonesBinding
import com.ailenezareti.panelnext.model.GeoZone
import com.ailenezareti.panelnext.model.ZoneDeleteRequest
import com.ailenezareti.panelnext.model.ZoneSaveRequest
import com.ailenezareti.panelnext.util.ChildResolver
import kotlinx.coroutines.launch

class ZonesFragment : Fragment() {

    private var _binding: FragmentZonesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ZoneAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentZonesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ZoneAdapter { zone -> confirmDelete(zone) }
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter
        binding.addBtn.setOnClickListener { showCreateZoneDialog() }

        loadZones()
    }

    private fun loadZones() {
        viewLifecycleOwner.lifecycleScope.launch {
            val childId = ChildResolver.id(requireContext())
            if (childId <= 0) {
                adapter.items = emptyList()
                return@launch
            }

            try {
                val response = ApiClient.get(requireContext()).zones(childId)
                adapter.items = response.body()?.zones.orEmpty()
            } catch (_: Exception) {
                adapter.items = emptyList()
            }
        }
    }

    private fun confirmDelete(zone: GeoZone) {
        AlertDialog.Builder(requireContext())
            .setTitle("${zone.name} silinsin?")
            .setMessage("Bu zona və onun radius ayarı silinəcək.")
            .setPositiveButton("Sil") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        ApiClient.get(requireContext()).deleteZone(
                            ZoneDeleteRequest(zone.id, zone.child_id)
                        )
                        loadZones()
                    } catch (_: Exception) {
                    }
                }
            }
            .setNegativeButton("Ləğv et", null)
            .show()
    }

    private fun showCreateZoneDialog() {
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 12, 40, 0)
        }

        fun field(hint: String, inputType: Int = InputType.TYPE_CLASS_TEXT): EditText {
            return EditText(context).apply {
                this.hint = hint
                this.inputType = inputType
                container.addView(this)
            }
        }

        val name = field("Zona adı")
        val latitude = field(
            "Latitude",
            InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL or
                InputType.TYPE_NUMBER_FLAG_SIGNED
        )
        val longitude = field(
            "Longitude",
            InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL or
                InputType.TYPE_NUMBER_FLAG_SIGNED
        )
        val radius = field("Radius (m)", InputType.TYPE_CLASS_NUMBER)

        AlertDialog.Builder(context)
            .setTitle("Yeni zona")
            .setView(container)
            .setPositiveButton("Yadda saxla") { _, _ ->
                val zoneName = name.text.toString().trim()
                val lat = latitude.text.toString().toDoubleOrNull()
                val lon = longitude.text.toString().toDoubleOrNull()
                val radiusMeters = radius.text.toString().toIntOrNull()

                if (zoneName.isEmpty() || lat == null || lon == null || radiusMeters == null) {
                    return@setPositiveButton
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    val childId = ChildResolver.id(context)
                    if (childId <= 0) return@launch

                    try {
                        ApiClient.get(context).createZone(
                            ZoneSaveRequest(
                                child_id = childId,
                                name = zoneName,
                                latitude = lat,
                                longitude = lon,
                                radius_m = radiusMeters,
                                notify_enter = true,
                                notify_exit = true
                            )
                        )
                        loadZones()
                    } catch (_: Exception) {
                    }
                }
            }
            .setNegativeButton("Ləğv et", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
