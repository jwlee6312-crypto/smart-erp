package com.crmbank.erp.mobile.hpio;
import android.os.Bundle;
import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.R;
public class MHPIO360S extends BaseActivity {
    @Override protected void onCreate(Bundle s) { super.onCreate(s); setContentView(R.layout.activity_template); }
    @Override protected String getProgramTitle() { return "생산일보"; }
    @Override protected String getProgramId() { return "MHPIO_360S"; }
}
