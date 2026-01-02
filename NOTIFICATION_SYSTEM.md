# Platisa Notification System Implementation

## Overview
Implemented a comprehensive notification system for Platisa that sends persistent system notifications to users about their bill payment obligations. All notifications are in Serbian Latin script and fully customizable by users.

## Features Implemented

### 1. **High Priority Notifications** ✅
- **Plaćanje dospeva za 3 dana** - Payment due in 3 days reminder
- **Plaćanje dospeva sutra** - Payment due tomorrow reminder  
- **Račun je prekoračio rok** - Overdue bill alert
- **Upozorenje o duplikatnom plaćanju** - Duplicate payment warning

### 2. **User Customization** ✅
- Toggle each notification type on/off individually
- Set preferred notification check time (hourly selector, default 9 AM)
- All preferences persist across app sessions
- Enabled by default for seamless user experience

### 3. **System Notifications** ✅
- Persistent Android system notifications (notification tray)
- Proper notification channels for different types
- Clickable notifications that open the app
- Auto-cancel on tap
- Proper notification importance levels (Default for due soon, High for overdue/duplicate)

## Architecture

### **Core Components**

1. **PreferenceManager** (`core/data/preferences/PreferenceManager.kt`)
   - Stores notification preferences using SharedPreferences
   - Properties: `notifyDue3Days`, `notifyDue1Day`, `notifyOverdue`, `notifyDuplicate`, `notificationTimeHour`

2. **PlatisaNotificationManager** (`core/notification/PlatisaNotificationManager.kt`)
   - Handles creating and showing system notifications
   - Creates notification channels (Cyrillic names in channels)
   - Methods for each notification type with proper Serbian text
   - Formats amounts and counts properly

3. **NotificationWorker** (`core/notification/NotificationWorker.kt`)
   - Background worker that checks bills daily
   - Calculates which bills are due in 3 days, 1 day, or overdue
   - Triggers appropriate notifications based on user preferences
   - Only checks visible, unpaid bills
   - Uses HiltWorker for dependency injection

4. **NotificationScheduler** (`core/notification/NotificationScheduler.kt`)
   - Schedules daily notification checks at user's preferred time
   - Calculates proper delay to next check time
   - Provides methods to reschedule when preferences change
   - Uses WorkManager with UNIQUE periodic work

### **UI Integration**

5. **SettingsViewModel** (`ui/screens/settings/SettingsViewModel.kt`)
   - Added StateFlows for all notification preferences
   - Toggle methods: `toggleNotifyDue3Days()`, `toggleNotifyDue1Day()`, etc.
   - `setNotificationTime(hour)` updates time and reschedules checks
   - Automatically schedules notifications on app start

6. **SettingsScreen** (`ui/screens/settings/SettingsScreen.kt`)
   - New "OBAVEŠTENJA" dropdown section
   - Toggle switches for each notification type with descriptions
   - Time selector button (cycles through 24 hours)
   - Consistent styling with existing settings sections
   - Uses AlertRed color for overdue/duplicate warnings

### **Resources**

7. **Notification Icon** (`res/drawable/ic_notification.xml`)
   - Simple bell vector drawable in CyberCyan color
   - Used by all system notifications

## User Flow

1. **App Launch**: Notification checks are automatically scheduled at preferred time (default 9 AM)

2. **Daily Check**: NotificationWorker runs every 24 hours and:
   - Queries all unpaid, visible bills from database
   - Checks if any bills match notification criteria (3 days, 1 day, overdue)
   - Shows appropriate system notifications if conditions are met
   - Respects user's notification preferences (only shows enabled types)

3. **User Customization**: 
   - User opens Settings → OBAVEŠTENJA
   - Toggles specific notification types on/off
   - Changes notification check time
   - Changes are saved immediately and scheduler updates

4. **Notification Tap**: User taps notification → Opens Platisa app

## Technical Details

### **Notification Channels**
- `bill_due_soon` - Default importance, cyan themed
- `bill_overdue` - High importance, red themed  
- `duplicate_warning` - High importance, red themed

### **Notification IDs**
- Due 3 days: 1001
- Due 1 day: 1002
- Overdue: 1003
- Duplicate: 1004

### **WorkManager Integration**
- Unique periodic work: "bill_notification_check"
- Runs every 24 hours
- Initial delay calculated based on preferred time
- Policy: UPDATE (replaces existing scheduled work)

### **Serbian Text Examples**
```kotlin
// Due in 3 days
"Imate 2 računa koji dospevaju za 3 dana (4,500.00 RSD)"

// Due tomorrow
"Imate 1 račun koji dospeva sutra (2,450.00 RSD)"

// Overdue
"⚠️ Prekoračeni računi!"
"Imate 3 računa koji su prekoračili rok (8,200.00 RSD)"

// Duplicate warning
"⚠️ Upozorenje o duplom plaćanju"
"Račun \"EPS Struja Decembar 2024\" je možda već plaćen."
```

## Dependencies Added

All required dependencies were already in place:
- WorkManager (already in use for GmailSync)
- Hilt (already configured)
- SharedPreferences (already in use)
- AndroidX Core/Compat (already in place)

## Future Enhancements (Not Implemented)

### **Bell Icon on Home Screen**
The user mentioned a bell icon on the main page. This could show:
- Badge count of active notifications
- Quick access to notification settings
- List of recent notifications
- Option to dismiss/snooze notifications

### **Additional Notification Types** (Lower Priority)
- New bill detected from Gmail sync
- STORNO bill detected  
- Unusual bill amount (significantly higher than average)
- Weekly/monthly summary
- Bills requiring manual attention

### **Advanced Features**
- Quiet hours (no notifications at night)
- Per-bill notification overrides
- Notification sound customization
- Vibration patterns
- Notification grouping (multiple bills in one notification)

## Testing Checklist

- [ ] Enable all notification types in Settings
- [ ] Check that notifications appear at scheduled time
- [ ] Verify proper Serbian text in notifications
- [ ] Test tapping notification opens app
- [ ] Disable specific notifications and verify they don't appear
- [ ] Change notification time and verify reschedule works
- [ ] Test with multiple bills (3 days, 1 day, overdue)
- [ ] Verify amount formatting is correct (RSD currency)
- [ ] Check notification channels appear in Android settings
- [ ] Test on different Android versions (especially 13+ with notification permission)

## Permissions

The app already has `POST_NOTIFICATIONS` permission in AndroidManifest.xml (required for Android 13+).

## Notes

- All notifications are persistent (stay until dismissed by user)
- Notification time preference is stored as 0-23 hour format
- Duplicate payment warning is implemented in the notification system but needs to be triggered from the payment flow (separate integration)
- The system respects user preferences - if a notification type is disabled, it won't be shown even if conditions are met
- Battery optimization warning already exists in Settings - users should disable battery optimization for reliable background notifications
