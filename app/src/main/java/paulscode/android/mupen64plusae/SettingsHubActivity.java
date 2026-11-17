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
package paulscode.android.mupen64plusae;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import java.io.File;

import paulscode.android.mupen64plusae.dialog.ConfirmationDialog;
import paulscode.android.mupen64plusae.dialog.LocaleDialog;
import paulscode.android.mupen64plusae.dialog.Popups;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.LocaleContextWrapper;

public class SettingsHubActivity extends AppCompatActivity implements ConfirmationDialog.PromptConfirmListener, View.OnClickListener {
    private static final int CLEAR_SHADER_CACHE_DIALOG_ID = 2;
    private static final String STATE_CLEAR_SHADERCACHE_DIALOG = "STATE_CLEAR_SHADERCACHE_DIALOG";
    private static final String STATE_LOCALE_DIALOG = "STATE_LOCALE_DIALOG";
    private static final String STATE_HARDWARE_INFO_POPUP = "STATE_HARDWARE_INFO_POPUP";
    private static final String STATE_SHOW_APP_VERSION_POPUP = "STATE_SHOW_APP_VERSION_POPUP";

    private GlobalPrefs mGlobalPrefs;

    @Override
    protected void attachBaseContext(Context newBase) {
        if (TextUtils.isEmpty(LocaleContextWrapper.getLocalCode())) {
            super.attachBaseContext(newBase);
        } else {
            super.attachBaseContext(LocaleContextWrapper.wrap(newBase, LocaleContextWrapper.getLocalCode()));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_hub_activity);

        mGlobalPrefs = new GlobalPrefs(this, new AppData(this));

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.menuItem_settings);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        bindRows();

        if (mGlobalPrefs.isBigScreenMode) {
            View rowTouchscreen = findViewById(R.id.rowTouchscreen);
            View rowTouchProfiles = findViewById(R.id.rowTouchProfiles);
            if (rowTouchscreen != null) rowTouchscreen.setVisibility(View.GONE);
            if (rowTouchProfiles != null) rowTouchProfiles.setVisibility(View.GONE);
        }
    }

    private void bindRows() {
        int[] rowIds = new int[]{
                R.id.rowLibrary, R.id.rowDisplay, R.id.rowShaders, R.id.rowAudio, R.id.rowInput,
                R.id.rowTouchscreen, R.id.rowData, R.id.rowEmuProfiles, R.id.rowTouchProfiles,
                R.id.rowControllerProfiles, R.id.rowImportExport, R.id.rowLanguage, R.id.rowReportBug
        };
        for (int id : rowIds) {
            View row = findViewById(id);
            if (row != null) {
                row.setOnClickListener(this);
            }
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.rowLibrary) {
            ActivityHelper.startLibraryPrefsActivity(this);
        } else if (id == R.id.rowDisplay) {
            ActivityHelper.startDisplayPrefsActivity(this);
        } else if (id == R.id.rowShaders) {
            ActivityHelper.startShadersPrefsActivity(this);
        } else if (id == R.id.rowAudio) {
            ActivityHelper.startAudioPrefsActivity(this);
        } else if (id == R.id.rowTouchscreen) {
            ActivityHelper.startTouchscreenPrefsActivity(this);
        } else if (id == R.id.rowInput) {
            ActivityHelper.startInputPrefsActivity(this);
        } else if (id == R.id.rowData) {
            ActivityHelper.startDataPrefsActivity(this);
        } else if (id == R.id.rowEmuProfiles) {
            ActivityHelper.startManageEmulationProfilesActivity(this);
        } else if (id == R.id.rowTouchProfiles) {
            ActivityHelper.startManageTouchscreenProfilesActivity(this);
        } else if (id == R.id.rowControllerProfiles) {
            ActivityHelper.startManageControllerProfilesActivity(this);
        } else if (id == R.id.rowImportExport) {
            ActivityHelper.startImportExportActivity(this);
        } else if (id == R.id.rowLanguage) {
            final CharSequence title = getText(R.string.menuItem_localeOverride);
            final LocaleDialog localeDialog = LocaleDialog.newInstance(title.toString());
            final FragmentManager fm = getSupportFragmentManager();
            localeDialog.show(fm, STATE_LOCALE_DIALOG);
        } else if (id == R.id.rowReportBug) {
            final String packageName = getPackageName();
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.menuItem_refreshRoms) {
            startActivity(new Intent(this, ScanRomsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPromptDialogClosed(int id, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE && id == CLEAR_SHADER_CACHE_DIALOG_ID) {
            FileUtil.deleteFolder(new File(mGlobalPrefs.shaderCacheDir));
        }
    }
}
