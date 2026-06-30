package com.example.ui

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.LicenceProfile
import com.example.data.TripLog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookScreen(
    viewModel: LogbookViewModel,
    modifier: Modifier = Modifier
) {
    val trips by viewModel.allTrips.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Dialog sheets states
    var showAddTripDialog by varState { false }
    var showEditProfileDialog by varState { false }
    var showExportDialog by varState { false }

    // Filtering states
    var searchQuery by varState { "" }
    var filterCompliance by varState { FilterType.ALL }

    // Memoized metrics
    val totalKilometres = remember(trips) {
        trips.sumOf { it.distanceKm }
    }
    val totalHours = remember(trips) {
        val totalMins = trips.sumOf { it.durationMinutes }
        totalMins / 60f
    }
    val complianceRate = remember(trips, profile) {
        if (trips.isEmpty()) 100 else {
            val compliantCount = trips.count { it.checkCompliance(profile).first }
            (compliantCount * 100) / trips.size
        }
    }

    // Filtered Trip Lists
    val filteredTrips = remember(trips, searchQuery, filterCompliance, profile) {
        trips.filter { trip ->
            val matchesSearch = trip.route.contains(searchQuery, ignoreCase = true) ||
                    trip.purpose.contains(searchQuery, ignoreCase = true) ||
                    trip.vehicle.contains(searchQuery, ignoreCase = true)

            val isCompliant = trip.checkCompliance(profile).first
            val matchesFilter = when (filterCompliance) {
                FilterType.ALL -> true
                FilterType.COMPLIANT -> isCompliant
                FilterType.INFRACTIONS -> !isCompliant
            }

            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("logbook_scaffold"),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddTripDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Trip") },
                text = { Text("Log Trip") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("add_trip_fab")
            )
        },
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "Licence Logbook",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Extraordinary Licence Compliance Tracker",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showEditProfileDialog = true },
                        modifier = Modifier.testTag("edit_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AdminPanelSettings,
                            contentDescription = "Licence Conditions"
                        )
                    }
                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export Logs"
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("top_app_bar")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 88.dp) // Cushion for floating action button
            ) {
                // Style Hero Illustration Banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(24.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_road_banner_1782161512882),
                            contentDescription = "Road banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.2f),
                                            Color.Black.copy(alpha = 0.75f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                "Holder: ${profile.holderName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Extraordinary Permit: ${profile.licenceNumber} | Vehicle: ${profile.allowedVehicle}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                // Licence Conditions overview banner card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("conditions_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Court Approved Conditions",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = { showEditProfileDialog = true },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Modify")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                InfoItem(
                                    icon = Icons.Default.CalendarMonth,
                                    label = "Approved Days",
                                    value = profile.allowedDays,
                                    modifier = Modifier.weight(1f)
                                )
                                InfoItem(
                                    icon = Icons.Default.Schedule,
                                    label = "Approved Hours",
                                    value = "${profile.allowedStartTime} - ${profile.allowedEndTime}",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                InfoItem(
                                    icon = Icons.Default.DirectionsCar,
                                    label = "Allowed Vehicle",
                                    value = profile.allowedVehicle.ifBlank { "Any Registered" },
                                    modifier = Modifier.weight(1f)
                                )
                                InfoItem(
                                    icon = Icons.Default.WorkOutline,
                                    label = "Permitted Purpose",
                                    value = profile.allowedPurposes,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Key metrics score cards
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "Log Summary Metrics",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Total Distance Card
                            StatCard(
                                title = "Distance Driven",
                                value = String.format("%.0f km", totalKilometres.toFloat()),
                                icon = Icons.Outlined.Map,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.weight(1f)
                            )

                            // Total Time / Hour Target Progress Card
                            val hourText = if (profile.targetRequiredHours > 0) {
                                String.format("%.1f / %d hrs", totalHours, profile.targetRequiredHours)
                            } else {
                                String.format("%.1f hrs", totalHours)
                            }
                            StatCard(
                                title = "Total Logged",
                                value = hourText,
                                icon = Icons.Outlined.Timer,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.weight(1f)
                            )

                            // Adherence Card
                            val adherenceBgColor = when {
                                complianceRate >= 95 -> Color(0xFFE8F5E9)
                                complianceRate >= 80 -> Color(0xFFFFF3E0)
                                else -> Color(0xFFFFEBEE)
                            }
                            val adherenceTextColor = when {
                                complianceRate >= 95 -> Color(0xFF2E7D32)
                                complianceRate >= 80 -> Color(0xFFE65100)
                                else -> Color(0xFFC62828)
                            }
                            StatCard(
                                title = "Permit Compliance",
                                value = "$complianceRate%",
                                icon = Icons.Outlined.Gavel,
                                color = adherenceBgColor,
                                valueColor = adherenceTextColor,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Hours Progress bar if required
                        if (profile.targetRequiredHours > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val progress = (totalHours / profile.targetRequiredHours.toFloat()).coerceIn(0f, 1f)
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Required Hours Log Book Progress",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${(progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                // Filter Control and Section Search Row
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search logs by route/purpose/car...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_field"),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Segmented filter row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = filterCompliance == FilterType.ALL,
                                onClick = { filterCompliance = FilterType.ALL },
                                label = { Text("All Logs (${trips.size})") },
                                modifier = Modifier.testTag("filter_all")
                            )
                            FilterChip(
                                selected = filterCompliance == FilterType.COMPLIANT,
                                onClick = { filterCompliance = FilterType.COMPLIANT },
                                label = { Text("Compliant") },
                                modifier = Modifier.testTag("filter_compliant"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE8F5E9),
                                    selectedLabelColor = Color(0xFF2E7D32)
                                )
                            )
                            FilterChip(
                                selected = filterCompliance == FilterType.INFRACTIONS,
                                onClick = { filterCompliance = FilterType.INFRACTIONS },
                                label = { Text("Infractions ⚠") },
                                modifier = Modifier.testTag("filter_infractions"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFEBEE),
                                    selectedLabelColor = Color(0xFFC62828)
                                )
                            )
                        }
                    }
                }

                if (filteredTrips.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DirectionsCar,
                                contentDescription = "No driving logs",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No Logged Trips Decoded",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (searchQuery.isNotEmpty() || filterCompliance != FilterType.ALL) {
                                    "No logs match your active research filters. Try clearing inputs or changing selectors."
                                } else {
                                    "Your Extraordinary permit requires maintaining strict trip reports. Tap 'Log Trip' below to add your first driving entry!"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(filteredTrips, key = { it.id }) { trip ->
                        TripCard(
                            trip = trip,
                            profile = profile,
                            onDelete = { viewModel.deleteTrip(trip) }
                        )
                    }
                }
            }
        }
    }

    // Modal Sheet or Dialog: ADD NEW TRIP
    if (showAddTripDialog) {
        AddTripDialog(
            profile = profile,
            lastOdometer = trips.firstOrNull()?.endOdometer ?: profile.allowedVehicle.let { 0 }, // fallback or use sensible starting value
            onDismiss = { showAddTripDialog = false },
            onSave = { newTrip ->
                viewModel.addTrip(newTrip)
                showAddTripDialog = false
            }
        )
    }

    // Modal Sheet or Dialog: EDIT LICENCE PROFILE CONDITIONS
    if (showEditProfileDialog) {
        EditProfileDialog(
            profile = profile,
            onDismiss = { showEditProfileDialog = false },
            onSave = { updatedProfile ->
                viewModel.updateProfile(updatedProfile)
                showEditProfileDialog = false
            }
        )
    }

    // Modal Sheet or Dialog: EXPORT LOG
    if (showExportDialog) {
        ExportLogDialog(
            profile = profile,
            trips = trips,
            onDismiss = { showExportDialog = false },
            onShare = {
                val csvData = generateCsv(profile, trips)
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, csvData)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Export Driving Log")
                context.startActivity(shareIntent)
            },
            onCopy = {
                val csvData = generateCsv(profile, trips)
                clipboardManager.setText(AnnotatedString(csvData))
            }
        )
    }
}

enum class FilterType {
    ALL, COMPLIANT, INFRACTIONS
}

@Composable
fun InfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified
) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TripCard(
    trip: TripLog,
    profile: LicenceProfile,
    onDelete: () -> Unit
) {
    var expanded by varState { false }
    val (isCompliant, infractionReason) = remember(trip, profile) {
        trip.checkCompliance(profile)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { expanded = !expanded }
            .testTag("trip_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isCompliant) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // First row: Date and Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = "Date",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = formatDate(trip.date),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                // Compliance Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isCompliant) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isCompliant) "COMPLIANT" else "INFRACTION ⚠",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompliant) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Second row: Time schedule & Route
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${trip.startTime} - ${trip.endTime}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "(${trip.durationMinutes} mins)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${trip.distanceKm} km",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Route from/to text
            Text(
                text = trip.route,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) 5 else 1,
                overflow = TextOverflow.Ellipsis
            )

            // Animated expansion detail
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                ) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Infraction warning detailed description if any
                    if (!isCompliant) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Infraction details",
                                    tint = Color(0xFFC62828),
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        "Infraction Details",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFC62828)
                                    )
                                    Text(
                                        infractionReason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFD32F2F)
                                    )
                                }
                            }
                        }
                    }

                    // Extra detailed stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Odometer Records", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Start: ${trip.startOdometer} km  |  End: ${trip.endOdometer} km", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Vehicle Registered", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(trip.vehicle.uppercase(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Trip Purpose", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(trip.purpose, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)

                    if (trip.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Log Notes/Observations", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(trip.notes, style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Delete Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        var showConfirmDelete by varState { false }

                        if (showConfirmDelete) {
                            Text(
                                "Delete?",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .align(Alignment.CenterVertically)
                            )
                            FilledTonalButton(
                                onClick = onDelete,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Confirm")
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            TextButton(
                                onClick = { showConfirmDelete = false },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Cancel")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showConfirmDelete = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete Entry")
                            }
                        }
                    }
                }
            }
        }
    }
}

// Dialog Component for ADD NEW TRIP
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTripDialog(
    profile: LicenceProfile,
    lastOdometer: Int,
    onDismiss: () -> Unit,
    onSave: (TripLog) -> Unit
) {
    // Current date in YYYY-MM-DD
    val currentFormattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    var dateInput by varState { currentFormattedDate }
    var startTimeInput by varState { "08:00" }
    var endTimeInput by varState { "08:45" }
    var startOdoInput by varState { if (lastOdometer > 0) lastOdometer.toString() else "45250" }
    var endOdoInput by varState { if (lastOdometer > 0) (lastOdometer + 15).toString() else "45265" }
    var vehicleInput by varState { profile.allowedVehicle }
    var purposeInput by varState { profile.allowedPurposes.split(",").firstOrNull()?.trim() ?: "Employment" }
    var routeInput by varState { "" }
    var notesInput by varState { "" }

    var formError by varState { "" }

    val purposeOptions = remember {
        listOf("Employment", "Education", "Medical", "Court Mandated", "Other")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("add_trip_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Log Driving Entry",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (formError.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = formError,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Date
                OutlinedTextField(
                    value = dateInput,
                    onValueChange = { dateInput = it },
                    label = { Text("Trip Date (YYYY-MM-DD)") },
                    placeholder = { Text("e.g. 2026-06-22") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Time Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = startTimeInput,
                        onValueChange = { startTimeInput = it },
                        label = { Text("Start Time (HH:mm)") },
                        placeholder = { Text("08:00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTimeInput,
                        onValueChange = { endTimeInput = it },
                        label = { Text("End Time (HH:mm)") },
                        placeholder = { Text("08:45") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Quick increment timers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            endTimeInput = addMinutesToTime(startTimeInput, 15)
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("+15m", style = MaterialTheme.typography.labelSmall)
                    }
                    FilledTonalButton(
                        onClick = {
                            endTimeInput = addMinutesToTime(startTimeInput, 30)
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("+30m", style = MaterialTheme.typography.labelSmall)
                    }
                    FilledTonalButton(
                        onClick = {
                            endTimeInput = addMinutesToTime(startTimeInput, 60)
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("+1h", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Odometer Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = startOdoInput,
                        onValueChange = { startOdoInput = it },
                        label = { Text("Start Odo") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endOdoInput,
                        onValueChange = { endOdoInput = it },
                        label = { Text("End Odo") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Quick Odometer adjustments
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val startNum = startOdoInput.toIntOrNull() ?: 0
                    FilledTonalButton(
                        onClick = { endOdoInput = (startNum + 10).toString() },
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("+10 km", style = MaterialTheme.typography.labelSmall)
                    }
                    FilledTonalButton(
                        onClick = { endOdoInput = (startNum + 25).toString() },
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("+25 km", style = MaterialTheme.typography.labelSmall)
                    }
                    FilledTonalButton(
                        onClick = { endOdoInput = (startNum + 50).toString() },
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("+50 km", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Vehicle Plate
                OutlinedTextField(
                    value = vehicleInput,
                    onValueChange = { vehicleInput = it },
                    label = { Text("Vehicle Registration Plate") },
                    placeholder = { Text("e.g. 1ABC123") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Purpose selection
                Text(
                    "Purpose",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    purposeOptions.take(4).forEach { option ->
                        val isSelected = purposeInput.equals(option, ignoreCase = true)
                        SuggestionChip(
                            onClick = { purposeInput = option },
                            label = { Text(option, style = MaterialTheme.typography.labelSmall) },
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
                OutlinedTextField(
                    value = purposeInput,
                    onValueChange = { purposeInput = it },
                    placeholder = { Text("Or specify other custom purpose") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Route
                OutlinedTextField(
                    value = routeInput,
                    onValueChange = { routeInput = it },
                    label = { Text("Route / Travel Details") },
                    placeholder = { Text("e.g. Joondalup via Freeway to West Perth") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Notes
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Log Notes (Optional)") },
                    placeholder = { Text("Traffic delays, secondary routes, safe driver notes...") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("dialog_cancel")) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val odometerS = startOdoInput.toIntOrNull()
                            val odometerE = endOdoInput.toIntOrNull()

                            when {
                                dateInput.isBlank() || startTimeInput.isBlank() || endTimeInput.isBlank() -> {
                                    formError = "Missing required datetime bounds!"
                                }
                                odometerS == null || odometerE == null -> {
                                    formError = "Odometers must be valid numbers!"
                                }
                                odometerE < odometerS -> {
                                    formError = "End Odometer must be greater or equal to Start Odometer!"
                                }
                                vehicleInput.isBlank() -> {
                                    formError = "Please declare the driving vehicle platform!"
                                }
                                routeInput.isBlank() -> {
                                    formError = "Route log description cannot be empty!"
                                }
                                else -> {
                                    onSave(
                                        TripLog(
                                            date = dateInput.trim(),
                                            startTime = startTimeInput.trim(),
                                            endTime = endTimeInput.trim(),
                                            startOdometer = odometerS,
                                            endOdometer = odometerE,
                                            vehicle = vehicleInput.trim(),
                                            purpose = purposeInput.trim(),
                                            route = routeInput.trim(),
                                            notes = notesInput.trim()
                                        )
                                    )
                                }
                            }
                        },
                        modifier = Modifier.testTag("dialog_save")
                    ) {
                        Text("Save Entry")
                    }
                }
            }
        }
    }
}

// Dialog Component for EDIT PROFILE CONDITIONS
@Composable
fun EditProfileDialog(
    profile: LicenceProfile,
    onDismiss: () -> Unit,
    onSave: (LicenceProfile) -> Unit
) {
    var holderNameInput by varState { profile.holderName }
    var licenceNoInput by varState { profile.licenceNumber }
    var allowedDaysInput by varState { profile.allowedDays }
    var allowedStartTimeInput by varState { profile.allowedStartTime }
    var allowedEndTimeInput by varState { profile.allowedEndTime }
    var allowedVehicleInput by varState { profile.allowedVehicle }
    var allowedPurposesInput by varState { profile.allowedPurposes }
    var targetHoursInput by varState { profile.targetRequiredHours.toString() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("edit_profile_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Configure Licence Conditions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = holderNameInput,
                    onValueChange = { holderNameInput = it },
                    label = { Text("Licence Holder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = licenceNoInput,
                    onValueChange = { licenceNoInput = it },
                    label = { Text("Extraordinary Licence Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = allowedDaysInput,
                    onValueChange = { allowedDaysInput = it },
                    label = { Text("Approved Driving Days (Comma separated)") },
                    placeholder = { Text("Mon,Tue,Wed,Thu,Fri") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = allowedStartTimeInput,
                        onValueChange = { allowedStartTimeInput = it },
                        label = { Text("Curfew Start (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = allowedEndTimeInput,
                        onValueChange = { allowedEndTimeInput = it },
                        label = { Text("Curfew End (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = allowedVehicleInput,
                    onValueChange = { allowedVehicleInput = it },
                    label = { Text("Allowed Vehicle Plate") },
                    placeholder = { Text("e.g. 1ABC123") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = allowedPurposesInput,
                    onValueChange = { allowedPurposesInput = it },
                    label = { Text("Permitted Driving Purposes") },
                    placeholder = { Text("Employment, Medical, Education") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = targetHoursInput,
                    onValueChange = { targetHoursInput = it },
                    label = { Text("Required Target Hours Logged (0 if none)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("profile_cancel")) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val parsedHours = targetHoursInput.toIntOrNull() ?: 0
                            onSave(
                                LicenceProfile(
                                    holderName = holderNameInput.trim(),
                                    licenceNumber = licenceNoInput.trim(),
                                    allowedDays = allowedDaysInput.trim(),
                                    allowedStartTime = allowedStartTimeInput.trim(),
                                    allowedEndTime = allowedEndTimeInput.trim(),
                                    allowedVehicle = allowedVehicleInput.trim(),
                                    allowedPurposes = allowedPurposesInput.trim(),
                                    targetRequiredHours = parsedHours
                                )
                            )
                        },
                        modifier = Modifier.testTag("profile_save")
                    ) {
                        Text("Apply Conditions")
                    }
                }
            }
        }
    }
}

// Dialog Component for EXPORT / SHARE LOOPS
@Composable
fun ExportLogDialog(
    profile: LicenceProfile,
    trips: List<TripLog>,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit
) {
    val reportPreview = remember(profile, trips) {
        generateCsv(profile, trips)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("export_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    "Submit Log Reports",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    "This report is formatted as a comma-separated-values (CSV) layout. You can share it directly with legal representatives, police officers, or Department of Transport processors.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Scrollable text viewer representing preview of CSV block
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = reportPreview,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                var copyFeedback by varState { false }

                if (copyFeedback) {
                    Text(
                        "Copied to clipboard!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onShare,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_dialog_share")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share CSV")
                    }

                    FilledTonalButton(
                        onClick = {
                            onCopy()
                            copyFeedback = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_dialog_copy")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Text")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag("export_dialog_cancel")
                ) {
                    Text("Close Preview")
                }
            }
        }
    }
}

// Utility formatting functions
fun formatDate(dateStr: String): String {
    return try {
        val sdfParser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateObj = sdfParser.parse(dateStr)
        if (dateObj != null) {
            val sdfFormatter = SimpleDateFormat("MMM d, yyyy (EEEE)", Locale.getDefault())
            sdfFormatter.format(dateObj)
        } else {
            dateStr
        }
    } catch (e: Exception) {
        dateStr
    }
}

fun addMinutesToTime(timeStr: String, minutesToAdd: Int): String {
    return try {
        val parts = timeStr.split(":")
        if (parts.size >= 2) {
            val hours = parts[0].toIntOrNull() ?: 0
            val minutes = parts[1].toIntOrNull() ?: 0
            var newMinutes = minutes + minutesToAdd
            var newHours = hours + (newMinutes / 60)
            newMinutes %= 60
            newHours %= 24
            String.format("%02d:%02d", newHours, newMinutes)
        } else {
            timeStr
        }
    } catch (e: Exception) {
        timeStr
    }
}

fun generateCsv(profile: LicenceProfile, trips: List<TripLog>): String {
    val sb = StringBuilder()
    sb.append("EXTRAORDINARY LICENCE DRIVING LOG REPORT\n")
    sb.append("Holder Name,${profile.holderName}\n")
    sb.append("Licence Number,${profile.licenceNumber}\n")
    sb.append("Restricted Vehicle Plate,${profile.allowedVehicle}\n")
    sb.append("Approved Days,${profile.allowedDays}\n")
    sb.append("Approved Time Window,${profile.allowedStartTime} to ${profile.allowedEndTime}\n")
    sb.append("Permitted Purposes,${profile.allowedPurposes}\n")
    sb.append("\n")
    sb.append("Date,Start Time,End Time,Duration (Mins),Start Odo (km),End Odo (km),Distance (km),Vehicle,Purpose,Route,Compliance Status,Compliance Issues\n")
    trips.forEach { trip ->
        val (comp, reason) = trip.checkCompliance(profile)
        val statusStr = if (comp) "COMPLIANT" else "NON-COMPLIANT"
        val cleanReason = reason.replace("\n", " | ").replace(",", ";")
        sb.append("${trip.date},${trip.startTime},${trip.endTime},${trip.durationMinutes},${trip.startOdometer},${trip.endOdometer},${trip.distanceKm},${trip.vehicle},\"${trip.purpose}\",\"${trip.route}\",$statusStr,\"$cleanReason\"\n")
    }
    return sb.toString()
}

// Inline state creator to reduce recompositions we can use `remember { mutableStateOf(...) }` inside helper functions which is standard
@Composable
inline fun <T> varState(crossinline init: () -> T): MutableState<T> {
    return remember { mutableStateOf(init()) }
}
