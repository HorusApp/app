package br.com.horusapp.ui;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.io.IOException;
import java.util.ArrayList;

import br.com.horusapp.R;
import br.com.horusapp.network.ApiService;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener {

    private EditText txtName;
    private EditText txtEmail;
    private EditText txtPassword;
    private CheckBox chkNewAccount;
    private Button btEnter;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        checkPermissions();

        txtName = (EditText) findViewById(R.id.txt_name);
        txtEmail = (EditText) findViewById(R.id.txt_email);
        txtPassword = (EditText) findViewById(R.id.txt_password);
        chkNewAccount = (CheckBox) findViewById(R.id.chk_new_account);
        btEnter = (Button) findViewById(R.id.bt_enter);

        btEnter.setOnClickListener(this);
        chkNewAccount.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        if (checked) {
            txtName.setVisibility(View.VISIBLE);
            btEnter.setText(getString(R.string.signup));
        } else {
            txtName.setVisibility(View.GONE);
            btEnter.setText(getString(R.string.enter));
        }
    }

    @Override
    public void onClick(View view) {
        if (txtEmail.getText().toString().isEmpty()) {
            txtEmail.setError(getString(R.string.cant_be_blank));
            return;
        }

        if (txtPassword.getText().toString().isEmpty()) {
            txtPassword.setError(getString(R.string.cant_be_blank));
            return;
        }

        // New Account
        if (chkNewAccount.isChecked()) {
            if (txtName.getText().toString().isEmpty()) {
                txtName.setError(getString(R.string.cant_be_blank));
                return;
            }

            signUp(txtName.getText().toString(),
                    txtEmail.getText().toString(),
                    txtPassword.getText().toString());
        } else {
            // Login
            login(txtEmail.getText().toString(),
                    txtPassword.getText().toString());
        }
    }

    private void checkPermissions() {
        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                Toast.makeText(LoginActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                Toast.makeText(LoginActivity.this, "Permission Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
                finish();
            }
        };

        new TedPermission(this)
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
                .setPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .check();
    }

    private void login(final String email, String password) {
        showLoading();

        ApiService apiService = ApiService.retrofit.create(ApiService.class);

        Call<ResponseBody> call = apiService.login(email, password);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                hideLoading();

                if (!response.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, getString(R.string.login_invalid), Toast.LENGTH_LONG).show();
                    return;
                }

                try {
                    Intent intent = new Intent(LoginActivity.this, CameraActivity.class);
                    intent.putExtra(CameraActivity.PARAM_USER_EMAIL, email);
                    intent.putExtra(CameraActivity.PARAM_USER_TOKEN, response.body().string());

                    startActivity(intent);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                finish();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                hideLoading();
                Toast.makeText(LoginActivity.this, getString(R.string.login_invalid), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void signUp(String name, final String email, final String password) {
        showLoading();

        ApiService apiService = ApiService.retrofit.create(ApiService.class);

        Call<ResponseBody> call = apiService.signUp(name, password, email);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                login(email, password);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                hideLoading();
                Toast.makeText(LoginActivity.this, getString(R.string.signup_error), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading() {
        if (progressDialog == null || !progressDialog.isShowing())
            progressDialog = ProgressDialog.show(this, "", getString(R.string.loading), true);
    }

    private void hideLoading() {
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
    }
}
