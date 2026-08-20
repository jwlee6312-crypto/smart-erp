package com.crmbank.erp.mobile.hsqm;
import android.os.Bundle;
import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.R;
public class MHSQM100U extends BaseActivity {
    @Override protected void onCreate(Bundle s) { super.onCreate(s); setContentView(R.layout.activity_template); }
    @Override protected String getProgramTitle() { return "품질검사요청"; }
    @Override protected String getProgramId() { return "MHSQM_100U"; }
}
