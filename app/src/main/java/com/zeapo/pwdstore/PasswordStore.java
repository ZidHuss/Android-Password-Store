package com.zeapo.pwdstore;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.zeapo.pwdstore.crypto.PgpHandler;
import com.zeapo.pwdstore.utils.PasswordItem;
import com.zeapo.pwdstore.utils.PasswordRepository;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialItem;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Stack;


public class PasswordStore extends Activity  implements ToCloneOrNot.OnFragmentInteractionListener, PasswordFragment.OnFragmentInteractionListener {
    private int listState = 0;
    private Stack<Integer> scrollPositions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pwdstore);
        scrollPositions = new Stack<Integer>();
    }

    @Override
    public void onResume(){
        super.onResume();

        // create the repository static variable in PasswordRepository
        PasswordRepository.getRepository(new File(getFilesDir() + "/store/.git"));

        // re-check that there was no change with the repository state
        checkLocalRepository();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.pwdstore, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.user_pref:
                try {
                    Intent intent = new Intent(this, UserPreference.class);
                    startActivity(intent);
                } catch (Exception e) {
                    System.out.println("Exception caught :(");
                    e.printStackTrace();
                }
                return true;

            case R.id.referesh:
                PasswordFragment plist;
                if  (null !=
                        (plist = (PasswordFragment) getFragmentManager().findFragmentByTag("PasswordsList"))) {
                    plist.updateAdapter();
                }
                return true;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void getClone(View view){
        Intent intent = new Intent(this, GitClone.class);
        startActivity(intent);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    private void checkLocalRepository() {
//        final File localDir = new File(getFilesDir() + "/store/.git");
        checkLocalRepository(PasswordRepository.getWorkTree());
    }

    private void checkLocalRepository(File localDir) {
        int status = 0;

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        if (localDir.exists()) {
            File[] folders = localDir.listFiles();
            status = folders.length;
        }

        // either the repo is empty or it was not correctly cloned
        switch (status) {
            case 0:
                if(!localDir.equals(PasswordRepository.getWorkTree()))
                    break;

                ToCloneOrNot cloneFrag = new ToCloneOrNot();
                fragmentTransaction.replace(R.id.main_layout, cloneFrag, "ToCloneOrNot");
                fragmentTransaction.commit();
                break;
            default:
                PasswordFragment passFrag = new PasswordFragment();
                Bundle args = new Bundle();
                args.putString("Path", localDir.getAbsolutePath());

                if (!scrollPositions.isEmpty())
                    args.putInt("Position", scrollPositions.pop());
                else
                    args.putInt("Position", 0);

                passFrag.setArguments(args);

                if (fragmentManager.findFragmentByTag("PasswordsList") != null)
                    fragmentTransaction.addToBackStack("passlist");

                fragmentTransaction.replace(R.id.main_layout, passFrag, "PasswordsList");
                fragmentTransaction.commit();
        }
    }

    /** Stack the positions the different fragments were at */
    @Override
    public void savePosition(Integer position) {
        this.scrollPositions.push(position);
    }

    /* If an item is clicked in the list of passwords, this will be triggered */
    @Override
    public void onFragmentInteraction(PasswordItem item) {
        if (item.getType() == PasswordItem.TYPE_CATEGORY) {
            checkLocalRepository(item.getFile());
        } else {
            try {
                byte[] data = new byte[0];
                try {
                    data = FileUtils.readFileToByteArray(PasswordRepository.getFile(item.getName()));

                    Intent intent = new Intent(this, PgpHandler.class);
                    intent.putExtra("PGP-ID", FileUtils.readFileToString(PasswordRepository.getFile("/.gpg-id")));
                    intent.putExtra("NAME", item.getName());
                    intent.putExtra("FILE_PATH", PasswordRepository.getFile(item.getName()).getAbsolutePath());
                    startActivity(intent);

                } catch (IOException e) {
                    e.printStackTrace();
                }


            } catch (Exception e) {
//            TODO handle problems
                e.printStackTrace();
            }
        }
    }

}