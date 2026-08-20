package com.crmbank.erp.mobile.hsio;
import android.os.Bundle;
import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.R;
public class MHSIO570U extends BaseActivity {
    @Override protected void onCreate(Bundle s) { super.onCreate(s); setContentView(R.layout.activity_template); }
    @Override protected String getProgramTitle() { return "타계정출고"; }
    @Override protected String getProgramId() { return "MHSIO_570U"; }
}
