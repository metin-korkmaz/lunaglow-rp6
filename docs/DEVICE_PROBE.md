# RP6 hardware gate

Gate H is not passed until the diagnostics build runs on a physical Retroid
Pocket 6 and proves that the left and right stick rings can be controlled
independently without Magisk.

1. Install the current APK.
2. Complete onboarding and open Hardware diagnostics.
3. Run the read-only probe.
4. Select Share Probe Report and return the exported text.

The report identifies `PServerBinder`, LED-class nodes, channel metadata, and
permissions. LunaGlow's RP6 driver writes only the vendor's fixed
`joystick_led_light_picker_color` and `led_light_brightness_percent` settings;
the physical test confirms those settings reach the two stick rings correctly.
