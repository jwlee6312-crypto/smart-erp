package com.crmbank.erp.mobile.hsqm;
import android.os.Bundle;
import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.R;
public class MHSQM100S extends BaseActivity {
    @Override protected void onCreate(Bundle s) { super.onCreate(s); setContentView(R.layout.activity_template); }
    @Override protected String getProgramTitle() { return "검사요청현황"; }
    @Override protected String getProgramId() { return "MHSQM_100S"; }
}
