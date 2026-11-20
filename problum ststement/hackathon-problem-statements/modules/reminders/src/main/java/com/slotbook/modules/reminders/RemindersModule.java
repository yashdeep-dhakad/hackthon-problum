package com.slotbook.modules.reminders;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Sample module. Members opt in to reminders; they get a notification when a booking is confirmed,
 * and another shortly before the class.
 *
 * The host finds this class through META-INF/spring/...AutoConfiguration.imports and the module's
 * tables through db/modules. Nothing in core refers to this module.
 */
@AutoConfiguration
@ComponentScan
public class RemindersModule {
}
