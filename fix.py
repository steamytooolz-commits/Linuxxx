import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# I want to find the LaunchedEffect block and clean it up.
# Currently it looks like:
#                     LaunchedEffect(Unit) {
#                         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
#                             if (ContextCompat.checkSelfPermission(
#                                     context,
#                                     Manifest.permission.POST_NOTIFICATIONS
#                                 ) != PackageManager.PERMISSION_GRANTED
#                             ) {
#                                 notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
#                             }
#                         }
#                                     context.startActivity(intent)
#                                 } catch (e: Exception) {
#                                     val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
#                                     context.startActivity(intent)
#                                 }
#                             }
#                         }
#                     }

start = content.find('LaunchedEffect(Unit) {')
end = content.find('LinuxDashboardApp(', start)

replacement = """LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }
                    """

content = content[:start] + replacement + content[end:]

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)

