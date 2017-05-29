$adb = "f:\\\\android\\\\sdk\\\\platform-tools\\\\adb.exe";

while (!$done) {
	
	open(TMP, 'f:/android/sdk/platform-tools/adb.exe devices -l |');
	while (<TMP>) {
		if (m/sdk_google_aw_x86/) {
			my ($em) = m/^(emulator-\d+)/;
			print "emulator is  $em\n";
			$cmd1 = "$adb -s $em pull /sdcard/wearRoutes logs";

			print("Running $cmd1\n");
			system($cmd1);
			$done = 1;
		}
	}
	if (!$done) { sleep(10); }
}