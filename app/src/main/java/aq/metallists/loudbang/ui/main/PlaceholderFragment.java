package aq.metallists.loudbang.ui.main;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import aq.metallists.loudbang.LBService;
import aq.metallists.loudbang.MainActivity;
import aq.metallists.loudbang.R;
import aq.metallists.loudbang.cutil.CJarInterface;
import aq.metallists.loudbang.cutil.DBHelper;
import aq.metallists.loudbang.cutil.WSPRMessage;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private PageViewModel pageViewModel;

    public static PlaceholderFragment newInstance(int index) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel.class);
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);
    }

    public void onResume() {
        super.onResume();

        try {
            final TextView state = (TextView) this.getView().findViewById(R.id.statusLabel1);

            if (LBService.lastKnownState.length() > 0) {
                state.setText(LBService.lastKnownState);
            }
        } catch (Exception x) {
        }

    }

    BroadcastReceiver bs;

    public void onDestroyView() {
        if (this.bs != null) {
            try {
                LocalBroadcastManager.getInstance(this.getActivity()).unregisterReceiver(this.bs);
            } catch (Exception x) {

            }
        }

        if (this.sp != null)
            this.sp.unregisterOnSharedPreferenceChangeListener(this);

        super.onDestroyView();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_status, container, false);

        final ProgressBar pbbm = root.findViewById(R.id.progressBar);
        final TextView state = (TextView) root.findViewById(R.id.statusLabel1);
        final ToggleButton ltb = (ToggleButton) root.findViewById(R.id.launch_toggle_btn);

        if (LBService.lastKnownState.length() > 0) {
            state.setText(LBService.lastKnownState);
        }

        pbbm.setMax(32767);


        this.sp = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
        this.bs = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().contains("eme.eva.loudbang.state")) {
                    boolean isRunnin = isMyServiceRunning(LBService.class);
                    if (isRunnin) {
                        ltb.setChecked(true);
                    } else {
                        ltb.setChecked(false);
                    }

                    state.setText(intent.getStringExtra("eme.eva.loudbang.state"));
                } else {
                    pbbm.setProgress(intent.getIntExtra("eme.eva.loudbang.level", 50));
                }

            }
        };

        // eme.eva.loudbang.state
        IntentFilter intentActionFilter = new IntentFilter();
        intentActionFilter.addAction("eme.eva.loudbang.state");
        intentActionFilter.addAction("eme.eva.loudbang.recordlevel");

        LocalBroadcastManager.getInstance(this.getActivity()).registerReceiver(bs, intentActionFilter);


        ltb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> permissionsToRequest = new ArrayList<String>();

                if (ContextCompat.checkSelfPermission(root.getContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.RECORD_AUDIO);
                }

                if (PlaceholderFragment.this.sp.getBoolean("use_gps", false)) {
                    if (ContextCompat.checkSelfPermission(root.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
                    }
                }

                if (PlaceholderFragment.this.sp.getBoolean("use_celltowers", false)) {
                    if (ContextCompat.checkSelfPermission(root.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION);
                    }
                }


                if (permissionsToRequest.size() > 0) {
                    ActivityCompat.requestPermissions(PlaceholderFragment.this.getActivity(),
                            permissionsToRequest.toArray(new String[]{}),
                            0);

                    ltb.setChecked(false);
                    return;
                }


                if (!isMyServiceRunning(LBService.class)) {
                    PlaceholderFragment.this.getActivity().startService(new Intent(PlaceholderFragment.this.getActivity(), LBService.class));
                    ltb.setChecked(true);
                } else {
                    PlaceholderFragment.this.getActivity().stopService(new Intent(PlaceholderFragment.this.getActivity(), LBService.class));
                    ltb.setChecked(false);
                }

            }
        });

        if (isMyServiceRunning(LBService.class)) {
            ltb.setChecked(true);
        }

        TextView bandname = root.findViewById(R.id.lbl_band);
        bandname.setText(this.getBandName(sp.getString("band", Double.toString(10.1387))));

        TextView callsign = root.findViewById(R.id.lbl_callsign);
        callsign.setText(this.sp.getString("callsign", "R0TST"));

        TextView grid = root.findViewById(R.id.lbl_grid);
        grid.setText(this.sp.getString("gridsq", "LO16xh"));

        TextView txstate = root.findViewById(R.id.lbl_txstate);
        String rxtx_state = "";
        if (this.sp.getBoolean("use_tx", false)) {
            rxtx_state = getString(R.string.lbl_tx_enabled);
        } else {
            rxtx_state = getString(R.string.lbl_tx_disabled);
        }

        switch (this.sp.getString("ptt_ctl", "none")) {
            case "none":
                rxtx_state = rxtx_state.concat(getString(R.string.tbt_txptt_noptt));
                break;
            case "fbang_2":
                rxtx_state = rxtx_state.concat(getString(R.string.tbt_txptt_fc));
                break;
            case "fbang_1":
                rxtx_state = rxtx_state.concat(getString(R.string.tbt_txptt_rc));
                break;
            default:
                rxtx_state = rxtx_state.concat(", <ERR>");
        }

        rxtx_state = String.format(Locale.GERMAN, "%s, %d%%", rxtx_state,
                Integer.parseInt(this.sp.getString("tx_probability", "25"))
        );

        txstate.setText(rxtx_state);


        //set tracking timer forsome systems
        final TextView lblCurrentTime = root.findViewById(R.id.lblCurrentTime);
        final Timer tmr = new Timer();
        tmr.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Time today = new Time(Time.getCurrentTimezone());
                today.setToNow();
                final String out = String.format(Locale.GERMANY, "%02d.%02d.%04d %02d:%02d:%02d",
                        today.monthDay, today.month, today.year, today.hour, today.minute, today.second);

                Activity thisAct = getActivity();
                if(thisAct == null){
                    tmr.cancel();
                    tmr.purge();
                    return;
                }

                thisAct.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        lblCurrentTime.setText(out);

                        if (isMyServiceRunning(LBService.class)) {
                            ltb.setChecked(true);
                        } else {
                            ltb.setChecked(false);
                        }
                    }
                });
            }
        }, 1000, 1000);


        sp.registerOnSharedPreferenceChangeListener(this);
        return root;
    }


    private String getBandName(String freq) {
        String[] freqs = this.getActivity().getResources().getStringArray(R.array.sets_bandarr_value);
        String[] bands = this.getActivity().getResources().getStringArray(R.array.sets_bandarr_name);

        if (freqs.length != bands.length) {
            return "<ERROR>";
        }

        for (int i = 0; i < freqs.length; i++) {
            if (freqs[i].equals(freq)) {
                return bands[i];
            }
        }

        return freq;
    }

    private SharedPreferences sp;

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) this.getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        try {
            switch (key) {
                case "band":
                    TextView bandname = this.getView().findViewById(R.id.lbl_band);
                    if (bandname != null)
                        bandname.setText(this.getBandName(sp.getString("band", Double.toString(10.1387))));
                    break;
                case "callsign":
                    TextView callsign = this.getView().findViewById(R.id.lbl_callsign);
                    if (callsign != null)
                        callsign.setText(this.sp.getString("callsign", "R0TST"));
                    break;
                case "gridsq":
                    TextView grid = this.getView().findViewById(R.id.lbl_grid);
                    if (grid != null)
                        grid.setText(this.sp.getString("gridsq", "LO16xh"));
                    break;
                case "use_tx":
                case "tx_probability":
                case "ptt_ctl":
                    TextView txstate = this.getView().findViewById(R.id.lbl_txstate);
                    if (txstate == null)
                        break;
                    String rxtx_state = "";
                    if (this.sp.getBoolean("use_tx", false)) {
                        rxtx_state = getString(R.string.lbl_tx_enabled);
                    } else {
                        rxtx_state = getString(R.string.lbl_tx_disabled);
                    }

                    switch (this.sp.getString("ptt_ctl", "none")) {
                        case "none":
                            rxtx_state = rxtx_state.concat(getString(R.string.tbt_txptt_noptt));
                            break;
                        case "fbang_2":
                            rxtx_state = rxtx_state.concat(getString(R.string.tbt_txptt_fc));
                            break;
                        case "fbang_1":
                            rxtx_state = rxtx_state.concat(getString(R.string.tbt_txptt_rc));
                            break;
                        default:
                            rxtx_state = rxtx_state.concat(", <ERR>");
                    }

                    rxtx_state = String.format(Locale.GERMAN, "%s, %d%%", rxtx_state,
                            Integer.parseInt(this.sp.getString("tx_probability", "25"))
                    );

                    txstate.setText(rxtx_state);
                    break;
                default:
            }
        } catch (Exception x) {
            Log.e("ERROR", "Exception:", x);
        }
    }
}