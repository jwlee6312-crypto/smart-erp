package com.crmbank.erp.mobile.hppl;
import android.os.Bundle;
import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.R;
public class MHPPL150U extends BaseActivity {
    @Override protected void onCreate(Bundle s) { super.onCreate(s); setContentView(R.layout.activity_template); }
    @Override protected String getProgramTitle() { return "양산계획등록"; }
    @Override protected String getProgramId() { return "MHPPL_150U"; }
}
