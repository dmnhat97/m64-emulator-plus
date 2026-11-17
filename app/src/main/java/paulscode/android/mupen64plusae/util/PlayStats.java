/*
 * XM64 Emulator Plus, a modified version of Mupen64PlusAE
 * <p>
 * Copyright (C) 2026 XM64 Emulator Plus contributors
 * <p>
 * This file is part of XM64 Emulator Plus.
 * <p>
 * XM64 Emulator Plus is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 * XM64 Emulator Plus is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General
 * Public License along with XM64 Emulator Plus. If not, see <http://www.gnu.org/licenses/>.
 */
package paulscode.android.mupen64plusae.util;

import android.content.Context;
import android.text.format.DateUtils;

import java.io.File;

import paulscode.android.mupen64plusae.R;
import paulscode.android.mupen64plusae.persistent.ConfigFile;

/**
 * Per-game favorites and play time, kept in their own small config file so that the game process
 * (which records play time) and the library process (which records favorites) never overwrite the
 * ROM info cache.
 */
public final class PlayStats
{
    private static final String FILE_NAME = "playStats.cfg";
    private static final String KEY_FAVORITE = "favorite";
    private static final String KEY_SECONDS = "playSeconds";

    private PlayStats() {}

    public static ConfigFile load( Context context )
    {
        return new ConfigFile( new File( context.getFilesDir(), FILE_NAME ).getAbsolutePath() );
    }

    public static boolean isFavorite( ConfigFile stats, String md5 )
    {
        return "1".equals( stats.get( md5, KEY_FAVORITE ) );
    }

    public static long playSeconds( ConfigFile stats, String md5 )
    {
        final String value = stats.get( md5, KEY_SECONDS );
        if( value == null )
            return 0;
        try {
            return Math.max( 0, Long.parseLong( value ) );
        } catch( NumberFormatException e ) {
            return 0;
        }
    }

    public static synchronized void setFavorite( Context context, String md5, boolean favorite )
    {
        final ConfigFile stats = load( context );
        stats.put( md5, KEY_FAVORITE, favorite ? "1" : "0" );
        stats.save();
    }

    public static synchronized void addPlaySeconds( Context context, String md5, long seconds )
    {
        if( seconds <= 0 || md5 == null || md5.isEmpty() )
            return;
        final ConfigFile stats = load( context );
        stats.put( md5, KEY_SECONDS, Long.toString( playSeconds( stats, md5 ) + seconds ) );
        stats.save();
    }

    public static String formatDuration( Context context, long seconds )
    {
        final long minutes = seconds / 60;
        if( minutes < 1 )
            return context.getString( R.string.stat_duration_lessThanMinute );
        if( minutes < 60 )
            return context.getString( R.string.stat_duration_minutes, minutes );
        return context.getString( R.string.stat_duration_hoursMinutes, minutes / 60, minutes % 60 );
    }

    public static String formatLastPlayed( Context context, long epochSeconds )
    {
        return DateUtils.getRelativeTimeSpanString( epochSeconds * 1000L, System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE ).toString();
    }
}
