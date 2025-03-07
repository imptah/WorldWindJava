/*
 * Copyright 2006-2009, 2017, 2020 United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 * 
 * The NASA World Wind Java (WWJ) platform is licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * 
 * NASA World Wind Java (WWJ) also contains the following 3rd party Open Source
 * software:
 * 
 *     Jackson Parser – Licensed under Apache 2.0
 *     GDAL – Licensed under MIT
 *     JOGL – Licensed under  Berkeley Software Distribution (BSD)
 *     Gluegen – Licensed under Berkeley Software Distribution (BSD)
 * 
 * A complete listing of 3rd Party software notices and licenses included in
 * NASA World Wind Java (WWJ)  can be found in the WorldWindJava-v2.2 3rd-party
 * notices and licenses PDF found in code directory.
 */
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;

import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.*;
/**
 * This class of static methods provides the interface to logging for WorldWind components. Logging is performed via
 * {@link java.util.logging.Logger}. The default logger name is <code>gov.nasa.worldwind</code>. The logger name is
 * configurable via {@link gov.nasa.worldwind.Configuration}.
 *
 * @author tag
 * @version $Id: Logging.java 1171 2013-02-11 21:45:02Z dcollins $
 * @see gov.nasa.worldwind.Configuration
 * @see java.util.logging.Logger
 */
public class Logging
{
    protected static final String MESSAGE_BUNDLE_NAME = Logging.class.getPackage().getName() + ".MessageStrings";
    protected static final int MAX_MESSAGE_REPEAT = Configuration.getIntegerValue(AVKey.MAX_MESSAGE_REPEAT, 10);

    private Logging()
    {
    } // Prevent instantiation

    /**
     * Returns the WorldWind logger.
     *
     * @return The logger.
     */
    public static Logger logger()
    {
        try
        {
            // The Configuration singleton may not be established yet, so catch the exception that occurs if it's not
            // and use the default logger name.
            String loggerName = Configuration.getStringValue(AVKey.LOGGER_NAME, Configuration.DEFAULT_LOGGER_NAME);
            return logger(loggerName);
        }
        catch (Exception e)
        {
            return logger(Configuration.DEFAULT_LOGGER_NAME);
        }
    }

    /**
     * Returns a specific logger. Does not access {@link gov.nasa.worldwind.Configuration} to determine the configured
     * WorldWind logger.
     * <p>
     * This is needed by {@link gov.nasa.worldwind.Configuration} to avoid calls back into itself when its singleton
     * instance is not yet instantiated.
     *
     * @param loggerName the name of the logger to use.
     *
     * @return The logger.
     */
    public static Logger logger(String loggerName)
    {
        return Logger.getLogger(loggerName != null ? loggerName : "", MESSAGE_BUNDLE_NAME);
    }

    /**
     * Retrieves a message from the WorldWind message resource bundle.
     *
     * @param property the property identifying which message to retrieve.
     *
     * @return The requested message.
     */
    public static String getMessage(String property)
    {
        try
        {
            return (String) ResourceBundle.getBundle(MESSAGE_BUNDLE_NAME, Locale.getDefault()).getObject(property);
        }
        catch (Exception e)
        {
            String message = "Exception looking up message from bundle " + MESSAGE_BUNDLE_NAME;
            logger().log(java.util.logging.Level.SEVERE, message, e);
            return message;
        }
    }

    /**
     * Retrieves a message from the WorldWind message resource bundle formatted with a single argument. The argument is
     * inserted into the message via {@link java.text.MessageFormat}.
     *
     * @param property the property identifying which message to retrieve.
     * @param arg      the single argument referenced by the format string identified <code>property</code>.
     *
     * @return The requested string formatted with the argument.
     *
     * @see java.text.MessageFormat
     */
    public static String getMessage(String property, String arg)
    {
        return arg != null ? getMessage(property, (Object) arg) : getMessage(property);
    }

    /**
     * Retrieves a message from the WorldWind message resource bundle formatted with specified arguments. The arguments
     * are inserted into the message via {@link java.text.MessageFormat}.
     *
     * @param property the property identifying which message to retrieve.
     * @param args     the arguments referenced by the format string identified <code>property</code>.
     *
     * @return The requested string formatted with the arguments.
     *
     * @see java.text.MessageFormat
     */
    public static String getMessage(String property, Object... args)
    {
        String message;

        try
        {
            message = (String) ResourceBundle.getBundle(MESSAGE_BUNDLE_NAME, Locale.getDefault()).getObject(property);
        }
        catch (Exception e)
        {
            message = "Exception looking up message from bundle " + MESSAGE_BUNDLE_NAME;
            logger().log(Level.SEVERE, message, e);
            return message;
        }

        try
        {
            if (args == null) return message;
            
            MessageFormat mf = new MessageFormat(message);
            return mf.format(args);
        }
        catch (IllegalArgumentException e)
        {
            message = "Message arguments do not match format string: " + property;
            logger().log(Level.SEVERE, message, e);
            return message;
        }
    }

    /**
     * Indicates the maximum number of times the same log message should be repeated when generated in the same context,
     * such as within a loop over renderables when operations in the loop consistently fail.
     *
     * @return the maximum number of times to repeat a message.
     */
    public static int getMaxMessageRepeatCount()
    {
        return MAX_MESSAGE_REPEAT;
    }
}
