# IntelliJ CPU Usage Indicator plugin
        
CPU Usage Indicator in the status bar, just like the Memory Indicator.<br/>
Also contains actions for performance problems diagnostics.<br/>
- left click on the panel generates a thread dump (useful when the IDE is doing something on the background and you want to know what).<br/>
- configuration GUI for Performance Watcher (IDE's bundled automatic thread dumper for frozen UI). 
You can configure it to dump even for shorter freezes than is the default: 5 second.  
<br/>

Note:<br/>
-it is painted in a background thread, not EDT, so it will update even when the GUI is stuck<br/>
-getting the process usage is a quite expensive operation (5ms on i7-6700k@4,2GHz on Win10, 100x more than getting the system usage) :(<br/>


![screenshot](https://github.com/krasa/CpuUsageIndicator/blob/master/cpuUsage.png)

![screenshot](https://github.com/krasa/CpuUsageIndicator/blob/master/dumps.png)


This modify version changed this:
- Move indicator after "FatalError", before "Memory" widget (see LangExtensions.xml statusBarWidgetFactory tag).
- Modify color scheme same as Memory widget (use it named color make nicely in any theme).
- Use TextPanel or not JButton to use normal font size of cpu usage percent.
- Remove border.

![screenshot](https://github.com/IceLitty/CpuUsageIndicator/blob/master/cpuUsageImgModifySample1.jpg)

![screenshot](https://github.com/IceLitty/CpuUsageIndicator/blob/master/cpuUsageImgModifySample2.jpg)
