package appbox.ideastracker;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.opengl.Visibility;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.ColorInt;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.ToggleButton;

import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.thebluealliance.spectrum.SpectrumDialog;
import com.yarolegovich.lovelydialog.LovelyCustomDialog;
import com.yarolegovich.lovelydialog.LovelyStandardDialog;
import com.yarolegovich.lovelydialog.LovelyTextInputDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.Inflater;

import appbox.ideastracker.database.DataEntry;
import appbox.ideastracker.database.DatabaseHelper;
import appbox.ideastracker.database.TinyDB;
import appbox.ideastracker.listadapters.MyCustomAdapter;
import appbox.ideastracker.listadapters.MyListAdapter;
import appbox.ideastracker.recycleview.RecyclerOnClickListener;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper mDbHelper;

    private Drawer result = null;
    private Drawer append = null;
    private AccountHeader header = null;
    private Toolbar mToolbar;
    private FloatingActionButton mFab;
    private FragmentManager mFragmentManager;
    private NonSwipeableViewPager mViewPager;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private TabLayout tabLayout;
    private Dialog mMoveDialog;
    private Dialog mNewIdeaDialog;


    private TinyDB mTinyDB;
    private static final String PREF_KEY = "MyPrefKey";
    private int mPrimaryColor;
    private int mSecondaryColor;
    private int mTextColor;
    private ArrayList<Object> mProjects;
    private List<IProfile> mProfiles;
    private int mSelectedProfileIndex;
    private boolean mNoTable = false;

    private RadioGroup mRadioGroup;

    private int defaultPrimaryColor;
    private int defaultSecondaryColor;
    private int defaultTextColor;

    private PrimaryDrawerItem mColorItem1;
    private PrimaryDrawerItem mColorItem2;
    private PrimaryDrawerItem mColorItem3;


    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Default colors
        defaultPrimaryColor = getResources().getColor(R.color.md_indigo_500);
        defaultSecondaryColor = getResources().getColor(R.color.md_orange_500);
        defaultTextColor = getResources().getColor(R.color.md_white);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mFragmentManager = getSupportFragmentManager();

        //Get the database helper
        mDbHelper = DatabaseHelper.getInstance(this);

        //TABLES
        mTinyDB = new TinyDB(this);
        loadProjects();

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(mFragmentManager);
        ListFragment.setMainActivity(this);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (NonSwipeableViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // Set up the tab layout
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(mViewPager);

        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newIdeaDialog();
            }
        });

        //DRAWERS
        setUpDrawers(savedInstanceState);

    }

    private Drawer.OnDrawerItemClickListener profile_listener = new Drawer.OnDrawerItemClickListener() {
        @Override
        public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
            if (drawerItem != null && drawerItem instanceof IProfile) {
                mSelectedProfileIndex = mProfiles.indexOf(drawerItem);
                String tableName = ((IProfile) drawerItem).getName().getText(MainActivity.this);
                mToolbar.setTitle(tableName);
                mDbHelper.switchTable(tableName);
                switchToProjectColors();
            }
            return false;
        }
    };

    private AccountHeader.OnAccountHeaderListener header_listener = new AccountHeader.OnAccountHeaderListener() {
        @Override
        public boolean onProfileChanged(View view, IProfile profile, boolean current) {
            return true;
        }
    };

    private void setUpDrawers(Bundle savedInstanceState) {

        //HEADER
        header = new AccountHeaderBuilder()
                .withActivity(this)
                .withOnAccountHeaderListener(header_listener)
                .withHeaderBackground(R.drawable.header)
                .withProfiles(mProfiles)
                .withProfileImagesVisible(false)
                .withSavedInstance(savedInstanceState)
                .build();

        //LEFT DRAWER
        result = new DrawerBuilder(this)
                .withToolbar(mToolbar)
                .withActionBarDrawerToggleAnimated(true)
                .withSelectedItem(-1)
                .withAccountHeader(header)
                .addDrawerItems(
                        new PrimaryDrawerItem().withIdentifier(1).withName("Rename project").withIcon(FontAwesome.Icon.faw_i_cursor).withSelectable(false),
                        new PrimaryDrawerItem().withIdentifier(2).withName("Delete project").withIcon(FontAwesome.Icon.faw_trash).withSelectable(false),
                        new DividerDrawerItem(),
                        new PrimaryDrawerItem().withIdentifier(4).withName("All projects").withIcon(GoogleMaterial.Icon.gmd_inbox).withSelectable(false),
                        new PrimaryDrawerItem().withIdentifier(3).withName("New project").withIcon(FontAwesome.Icon.faw_plus).withSelectable(false)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerItem != null) {
                            int id = (int) drawerItem.getIdentifier();
                            switch (id) {
                                case 1:
                                    renameTableDialog();
                                    break;

                                case 2:
                                    deleteTableDialog();
                                    break;

                                case 3:
                                    newTableDialog();
                                    break;

                                case 4:
                                    header.toggleSelectionList(getApplicationContext());
                                    break;
                            }
                        }
                        return true;
                    }
                })
                .withSavedInstance(savedInstanceState)
                .build();

        mColorItem1 = new PrimaryDrawerItem().withIdentifier(1).withName("Primary color").withIcon(FontAwesome.Icon.faw_paint_brush).withIconColor(mPrimaryColor).withSelectable(false);
        mColorItem2 = new PrimaryDrawerItem().withIdentifier(2).withName("Secondary color").withIcon(FontAwesome.Icon.faw_paint_brush).withIconColor(mSecondaryColor).withSelectable(false);
        mColorItem3 = new PrimaryDrawerItem().withIdentifier(3).withName("Text color").withIcon(FontAwesome.Icon.faw_paint_brush).withIconColor(mTextColor).withSelectable(false);

        //RIGHT DRAWER
        append = new DrawerBuilder(this)
                .withActionBarDrawerToggleAnimated(true)
                .withSelectedItem(-1)
                .addDrawerItems(
                        new SectionDrawerItem().withName("Color preferences"),
                        mColorItem1,
                        mColorItem2,
                        mColorItem3,
                        new PrimaryDrawerItem().withIdentifier(6).withName("Reset color preferences").withIcon(FontAwesome.Icon.faw_tint).withSelectable(false),
                        new SectionDrawerItem().withName("Functions"),
                        new PrimaryDrawerItem().withIdentifier(4).withName("Move all ideas from a tab").withIcon(FontAwesome.Icon.faw_exchange).withSelectable(false),
                        new PrimaryDrawerItem().withIdentifier(5).withName("Expand/collapse all").withIcon(FontAwesome.Icon.faw_arrows_v).withSelectable(false)
                )
                .withDrawerGravity(Gravity.END)
                .withStickyFooter(R.layout.footer)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {

                        if (drawerItem != null && !mNoTable) {
                            int id = (int) drawerItem.getIdentifier();
                            switch (id) {
                                case 1:
                                    new SpectrumDialog.Builder(getApplicationContext())
                                            .setTitle("Select primary color")
                                            .setColors(R.array.colors)
                                            .setSelectedColor(mPrimaryColor)
                                            .setDismissOnColorSelected(false)
                                            .setFixedColumnCount(4)
                                            .setOnColorSelectedListener(new SpectrumDialog.OnColorSelectedListener() {
                                                @Override
                                                public void onColorSelected(boolean positiveResult, @ColorInt int color) {
                                                    if (positiveResult) {
                                                        //update selected color
                                                        mPrimaryColor = color;
                                                        changePrimaryColor();
                                                    }
                                                }
                                            }).build().show(mFragmentManager, "dialog_spectrum");

                                    break;

                                case 2:
                                    new SpectrumDialog.Builder(getApplicationContext())
                                            .setTitle("Select secondary color")
                                            .setColors(R.array.colors)
                                            .setSelectedColor(mSecondaryColor)
                                            .setDismissOnColorSelected(false)
                                            .setFixedColumnCount(4)
                                            .setOnColorSelectedListener(new SpectrumDialog.OnColorSelectedListener() {
                                                @Override
                                                public void onColorSelected(boolean positiveResult, @ColorInt int color) {
                                                    if (positiveResult) {
                                                        //update selected color
                                                        mSecondaryColor = color;
                                                        changeSecondaryColor();
                                                    }
                                                }
                                            }).build().show(mFragmentManager, "dialog_spectrum");
                                    break;

                                case 3:
                                    new SpectrumDialog.Builder(getApplicationContext())
                                            .setTitle("Select text color")
                                            .setColors(R.array.textColors)
                                            .setSelectedColor(mTextColor)
                                            .setDismissOnColorSelected(false)
                                            .setFixedColumnCount(4)
                                            .setOutlineWidth(2)
                                            .setOnColorSelectedListener(new SpectrumDialog.OnColorSelectedListener() {
                                                @Override
                                                public void onColorSelected(boolean positiveResult, @ColorInt int color) {
                                                    if (positiveResult) {
                                                        //update selected color
                                                        mTextColor = color;
                                                        changeTextColor();
                                                    }
                                                }
                                            }).build().show(mFragmentManager, "dialog_spectrum");
                                    break;

                                case 4:
                                    newMoveDialog();
                                    append.closeDrawer();
                                    break;

                                case 5:
                                    AnimatedExpandableListView.getInstance().collapseExpandAll();
                                    break;

                                case 6:
                                    resetColorsDialog();
                                    break;
                            }
                        } else {
                            //TODO: create a table message
                        }
                        return true;
                    }
                })
                .withSavedInstance(savedInstanceState)
                .append(result);

        //Select first one
        mSelectedProfileIndex = 0;
        IProfile activeProfile = mProfiles.get(mSelectedProfileIndex);
        String activeProfileName = activeProfile.getName().getText();
        header.setActiveProfile(activeProfile);
        getSupportActionBar().setTitle(activeProfileName);
        DataEntry.setTableName(activeProfileName);

        switchToProjectColors();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //add the values which need to be saved from the drawer to the bundle
        outState = result.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        //handle the back press :D close the drawer first and if the drawer is closed close the activity
        if (result != null && result.isDrawerOpen()) {
            result.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

    public void newIdeaDialog() {

        mNewIdeaDialog = new LovelyCustomDialog(this, R.style.EditTextTintTheme)
                .setView(R.layout.new_idea_form)
                .setTopColor(mPrimaryColor)
                .setTitle("New idea")
                .setIcon(R.drawable.ic_bulb)
                .setListener(R.id.doneButton, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Switch doLater = (Switch) mNewIdeaDialog.findViewById(R.id.doLater);
                        RadioGroup radioGroup = (RadioGroup) mNewIdeaDialog.findViewById(R.id.radioGroup);
                        EditText ideaField = (EditText) mNewIdeaDialog.findViewById(R.id.editText);

                        if (radioGroup.getCheckedRadioButtonId() != -1) {
                            View radioButton = radioGroup.findViewById(radioGroup.getCheckedRadioButtonId());
                            RadioButton btn = (RadioButton) radioGroup.getChildAt(radioGroup.indexOfChild(radioButton));
                            String selection = (String) btn.getText();

                            String text = ideaField.getText().toString();
                            boolean later = doLater.isChecked();
                            int priority = Integer.parseInt(selection);

                            mDbHelper.newEntry(text, priority, later); //add the idea to the actual database

                            DatabaseHelper.notifyAllLists();
                        }

                        mNewIdeaDialog.dismiss();
                    }
                })
                .show();

    }

    public void newIdeaDialog(int priority) {

        mNewIdeaDialog = new LovelyCustomDialog(this, R.style.EditTextTintTheme)
                .setView(R.layout.new_idea_form)
                .setTopColor(mPrimaryColor)
                .setTitle("New idea")
                .setIcon(R.drawable.ic_bulb)
                .setListener(R.id.doneButton, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Switch doLater = (Switch) mNewIdeaDialog.findViewById(R.id.doLater);
                        EditText ideaField = (EditText) mNewIdeaDialog.findViewById(R.id.editText);

                        if (mRadioGroup.getCheckedRadioButtonId() != -1) {
                            View radioButton = mRadioGroup.findViewById(mRadioGroup.getCheckedRadioButtonId());
                            RadioButton btn = (RadioButton) mRadioGroup.getChildAt(mRadioGroup.indexOfChild(radioButton));
                            String selection = (String) btn.getText();

                            String text = ideaField.getText().toString();
                            boolean later = doLater.isChecked();
                            int priority = Integer.parseInt(selection);

                            mDbHelper.newEntry(text, priority, later); //add the idea to the actual database

                            DatabaseHelper.notifyAllLists();
                        }

                        mNewIdeaDialog.dismiss();
                    }
                })
                .show();

        mRadioGroup = (RadioGroup) mNewIdeaDialog.findViewById(R.id.radioGroup);
        RadioButton radio = null;
        switch (priority) {
            case 1:
                radio = (RadioButton) mNewIdeaDialog.findViewById(R.id.radioButton1);
                break;
            case 2:
                radio = (RadioButton) mNewIdeaDialog.findViewById(R.id.radioButton2);
                break;
            case 3:
                radio = (RadioButton) mNewIdeaDialog.findViewById(R.id.radioButton3);
                break;
        }
        radio.setChecked(true);


    }

    private void newMoveDialog() {

        final View root = findViewById(R.id.main_content);

        mMoveDialog = new LovelyCustomDialog(this)
                .setView(R.layout.move_dialog)
                .setTopColor(mPrimaryColor)
                .setTitle("Move all ideas")
                .setTitleGravity(Gravity.CENTER_HORIZONTAL)
                .setIcon(R.drawable.ic_transfer)
                .setListener(R.id.move_button, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Spinner spinnerFrom = (Spinner) mMoveDialog.findViewById(R.id.spinner_from);
                        Spinner spinnerTo = (Spinner) mMoveDialog.findViewById(R.id.spinner_to);
                        final String from = spinnerFrom.getSelectedItem().toString();
                        final String to = spinnerTo.getSelectedItem().toString();

                        String snackText = "Nothing to move from " + from;
                        boolean success = false;
                        if (from.equals(to)) snackText = "Locations must be different";
                        else if (mDbHelper.moveAllFromTo(from, to)) {
                            snackText = "All ideas from " + from + " moved to " + to;
                            success = true;
                        }

                        Snackbar snackbar = Snackbar.make(root, snackText, Snackbar.LENGTH_LONG);
                        if (success) {
                            snackbar.setAction("UNDO", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if (to.equals("Trash")) {//undo temp deleting
                                        mDbHelper.recoverAllFromTemp();
                                    } else {
                                        mDbHelper.moveAllFromTo(to, from);
                                    }
                                }
                            }).setCallback(new Snackbar.Callback() {
                                @Override
                                public void onDismissed(Snackbar snackbar, int event) {
                                    if ((event == Snackbar.Callback.DISMISS_EVENT_TIMEOUT || event == Snackbar.Callback.DISMISS_EVENT_CONSECUTIVE) && to.equals("Trash")) {
                                        //delete for real ideas in temp
                                        mDbHelper.deleteAllFromTemp();
                                    }
                                }
                            });
                        }
                        mMoveDialog.dismiss();
                        snackbar.show();
                    }
                })
                .show();
    }

    private void newTableDialog() {

        new LovelyTextInputDialog(this, R.style.EditTextTintTheme)
                .setTopColor(mPrimaryColor)
                .setConfirmButtonColor(getResources().getColor(R.color.md_pink_a200))
                .setTitle("New project")
                .setMessage("Find your project an awesome name")
                .setIcon(R.drawable.ic_notepad)
                .setInputFilter("A project with this name already exists", new LovelyTextInputDialog.TextFilter() {
                    @Override
                    public boolean check(String text) {
                        return isProjectNameAvailable(text);
                    }
                })
                .setInputFilter("Try something longer", new LovelyTextInputDialog.TextFilter() {
                    @Override
                    public boolean check(String text) {
                        return !text.equals("");
                    }
                })
                .setConfirmButton(android.R.string.ok, new LovelyTextInputDialog.OnTextInputConfirmListener() {
                    @Override
                    public void onTextInputConfirmed(String tableName) {

                        mDbHelper.newTable(tableName);
                        IProfile newProfile = new ProfileDrawerItem().withName(tableName).withOnDrawerItemClickListener(profile_listener);
                        mProfiles.add(newProfile);

                        saveProject(new Project(tableName, defaultPrimaryColor, defaultSecondaryColor, defaultTextColor));

                        //open the profile drawer and select the new profile
                        header.setActiveProfile(newProfile);
                        mSelectedProfileIndex = mProfiles.size() - 1;
                        header.toggleSelectionList(getApplicationContext());
                        mToolbar.setTitle(tableName);
                        mFab.setVisibility(View.VISIBLE);
                        mNoTable = false;

                        mViewPager.setAdapter(null);
                        mViewPager.setAdapter(mSectionsPagerAdapter);
                    }
                })
                .show();
    }

    private void renameTableDialog() {

        new LovelyTextInputDialog(this, R.style.EditTextTintTheme)
                .setTopColor(mPrimaryColor)
                .setConfirmButtonColor(getResources().getColor(R.color.md_pink_a200))
                .setTitle("Rename " + ((Project) mProjects.get(mSelectedProfileIndex)).getName())
                .setMessage("A new name for a fresh start.")
                .setIcon(R.drawable.ic_edit)
                .setInputFilter("A project with this name already exists", new LovelyTextInputDialog.TextFilter() {
                    @Override
                    public boolean check(String text) {
                        return isProjectNameAvailable(text);
                    }
                })
                .setInputFilter("Try something longer", new LovelyTextInputDialog.TextFilter() {
                    @Override
                    public boolean check(String text) {
                        return !text.equals("");
                    }
                })
                .setConfirmButton(android.R.string.ok, new LovelyTextInputDialog.OnTextInputConfirmListener() {
                    @Override
                    public void onTextInputConfirmed(String tableName) {
                        //update table's name is the list and the database
                        renameProject(tableName);
                        mDbHelper.renameTable(tableName);

                        //update profile's name
                        IProfile profile = mProfiles.get(mSelectedProfileIndex);
                        profile.withName(tableName);
                        header.updateProfile(profile);
                        mProfiles.remove(mSelectedProfileIndex);
                        mProfiles.add(mSelectedProfileIndex, profile);

                        mToolbar.setTitle(tableName);
                    }
                })
                .show();
    }

    private void deleteTableDialog() {
        new LovelyStandardDialog(this)
                .setTopColorRes(R.color.md_red_400)
                .setButtonsColorRes(R.color.md_deep_orange_500)
                .setIcon(R.drawable.ic_warning)
                .setTitle("Delete project '" + ((Project) mProjects.get(mSelectedProfileIndex)).getName() + "'")
                .setMessage("I agree, it was not that good anyway.")
                .setPositiveButton(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        mProfiles.remove(mSelectedProfileIndex);
                        deleteProject();
                        mDbHelper.deleteTable();
                        if (mProjects.isEmpty()) {
                            DataEntry.setTableName("");
                            mToolbar.setTitle(R.string.app_name);
                            mFab.setVisibility(View.INVISIBLE);
                            header.setProfiles(mProfiles);
                            mNoTable = true;

                            mViewPager.setAdapter(null);
                            mViewPager.setAdapter(mSectionsPagerAdapter);
                        }
                        switchToExistingTable(mSelectedProfileIndex);
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void resetColorsDialog() {
        new LovelyStandardDialog(this)
                .setTopColor(mPrimaryColor)
                .setButtonsColorRes(R.color.md_pink_a200)
                .setIcon(R.drawable.ic_drop)
                .setTitle("Reset color preferences")
                .setMessage("The color preferences for this project will be reset to the default ones.")
                .setPositiveButton(android.R.string.yes, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mPrimaryColor = defaultPrimaryColor;
                        mSecondaryColor = defaultSecondaryColor;
                        mTextColor = defaultTextColor;
                        updateColors();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void switchToExistingTable(int index) {
        index -= 1;
        boolean inBounds = (index >= 0) && (index < mProfiles.size());

        if (!mProfiles.isEmpty()) {

            if (inBounds) mSelectedProfileIndex = index;
            else mSelectedProfileIndex = 0;

            IProfile profileToSelect = mProfiles.get(mSelectedProfileIndex);
            String tableToSelect = profileToSelect.getName().getText();
            header.setActiveProfile(profileToSelect);
            mToolbar.setTitle(tableToSelect);
            mDbHelper.switchTable(tableToSelect);

            switchToProjectColors();
        } else {
            //TODO: No table to show
        }

    }


    @SuppressWarnings("ConstantConditions")
    private void changePrimaryColor() {

        saveProjectColors();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        AppBarLayout appbar = (AppBarLayout) findViewById(R.id.appbar);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);

        toolbar.setBackgroundColor(mPrimaryColor);
        tabLayout.setBackgroundColor(mPrimaryColor);
        appbar.setBackgroundColor(mPrimaryColor);

        if (Build.VERSION.SDK_INT >= 21) {
            //getWindow().setNavigationBarColor(getResources().getColor(R.color.colorPrimaryDark));
            getWindow().setStatusBarColor(darken(mPrimaryColor));
        }

        mColorItem1.withIconColor(mPrimaryColor);
        append.updateItem(mColorItem1);

        RecyclerOnClickListener.setPrimaryColor(mPrimaryColor);
    }

    private void changeSecondaryColor() {

        saveProjectColors();

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);

        tabLayout.setSelectedTabIndicatorColor(mSecondaryColor);
        mFab.setBackgroundTintList(ColorStateList.valueOf(mSecondaryColor));

        mColorItem2.withIconColor(mSecondaryColor);
        append.updateItem(mColorItem2);
    }

    private void changeTextColor() {

        saveProjectColors();

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);

        tabLayout.setTabTextColors(darken(mTextColor), mTextColor);
        mToolbar.setTitleTextColor(mTextColor);

        ToolbarColorizeHelper.colorizeToolbar(mToolbar, mTextColor, this);

        mColorItem3.withIconColor(mTextColor);
        append.updateItem(mColorItem3);

    }

    private void updateColors() {
        changePrimaryColor();
        changeSecondaryColor();
        changeTextColor();
    }

    private void switchToProjectColors() {
        Project selectedProject = (Project) mProjects.get(mSelectedProfileIndex);
        mPrimaryColor = selectedProject.getPrimaryColor();
        mSecondaryColor = selectedProject.getSecondaryColor();
        mTextColor = selectedProject.getTextColor();

        updateColors();

        mColorItem1.withIconColor(mPrimaryColor);
        mColorItem2.withIconColor(mSecondaryColor);
        mColorItem3.withIconColor(mTextColor);

        append.updateItem(mColorItem1);
        append.updateItem(mColorItem2);
        append.updateItem(mColorItem3);

    }

    private int darken(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.85f;
        color = Color.HSVToColor(hsv);
        return color;
    }

    private void saveProject(Project p) {

        if (mProjects == null) {
            mProjects = new ArrayList<>();
        }
        mProjects.add(p);

        // save the project list to preference
        mTinyDB.putListObject(PREF_KEY, mProjects);

    }

    private void saveProjectColors() {
        Project p = (Project) mProjects.get(mSelectedProfileIndex);
        p.setPrimaryColor(mPrimaryColor);
        p.setSecondaryColor(mSecondaryColor);
        p.setTextColor(mTextColor);
        mTinyDB.putListObject(PREF_KEY, mProjects);
    }

    private void renameProject(String newName) {
        Project p = (Project) mProjects.get(mSelectedProfileIndex);
        p.setName(newName);
        mTinyDB.putListObject(PREF_KEY, mProjects);
    }

    private boolean isProjectNameAvailable(String name) {

        for (Object o : mProjects) {
            Project p = (Project) o;
            if (p.getName().equalsIgnoreCase(name)) return false;
        }
        return true;
    }

    private void deleteProject() {
        mProjects.remove(mSelectedProfileIndex);
        mTinyDB.putListObject(PREF_KEY, mProjects);
    }

    private void loadProjects() {

        mProjects = mTinyDB.getListObject(PREF_KEY, Project.class);
        if (mProjects.size() == 0) {
            saveProject(new Project("MyProject",
                    defaultPrimaryColor,
                    defaultSecondaryColor,
                    defaultTextColor));
            mDbHelper.newTable("MyProject");
        }

        mProfiles = new ArrayList<>();
        for (Object p : mProjects) {
            Project project = (Project) p;
            mProfiles.add(new ProfileDrawerItem().withName(project.getName()).withOnDrawerItemClickListener(profile_listener));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!mNoTable) append.openDrawer();
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class ListFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */

        private static MainActivity mainActivity;

        public static ListFragment newInstance(int index) {
            ListFragment f = new ListFragment();

            // Supply index input as an argument.
            Bundle args = new Bundle();
            args.putInt("index", index);
            f.setArguments(args);

            return f;
        }

        public static void setMainActivity(MainActivity act) {
            mainActivity = act;
        }

        public int getIndex() {
            return getArguments().getInt("index", 0);
        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = null;
            if (DataEntry.TABLE_NAME.equals("[]")) {
                return inflater.inflate(R.layout.no_project_layout, container, false);
            }

            switch (this.getIndex()) {
                case 0: //IDEAS
                    rootView = inflater.inflate(R.layout.fragment_main, container, false);
                    AnimatedExpandableListView list = (AnimatedExpandableListView) rootView.findViewById(R.id.expandableList);
                    //sets the adapter that provides data to the list
                    MyCustomAdapter adapter = new MyCustomAdapter(getContext());
                    DatabaseHelper.setAdapterIdea(adapter);
                    list.setAdapter(adapter);
                    list.expandGroup(0);
                    list.expandGroup(1);
                    list.expandGroup(2);
                    setListeners(list);

                    break;

                case 1: //LATER
                    rootView = inflater.inflate(R.layout.fragment_secondary, container, false);
                    ListView list2 = (ListView) rootView.findViewById(R.id.list);
                    MyListAdapter adapter2 = new MyListAdapter(getContext(), true);
                    DatabaseHelper.setAdapterLater(adapter2);
                    list2.setAdapter(adapter2);
                    break;

                case 2: //DONE
                    rootView = inflater.inflate(R.layout.fragment_secondary, container, false);
                    ListView list3 = (ListView) rootView.findViewById(R.id.list);
                    MyListAdapter adapter3 = new MyListAdapter(getContext(), false);
                    DatabaseHelper.setAdapterDone(adapter3);
                    list3.setAdapter(adapter3);
                    break;

            }


            return rootView;
        }

        void setListeners(final AnimatedExpandableListView listView) {
            listView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {

                @Override
                public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {

                    if (listView.getExpandableListAdapter().getChildrenCount(groupPosition) != 0) { //group is not empty
                        if (listView.isGroupExpanded(groupPosition)) {
                            listView.collapseGroupWithAnimation(groupPosition);
                        } else {
                            listView.expandGroupWithAnimation(groupPosition);
                        }
                    } else { //group is empty
                        mainActivity.newIdeaDialog(groupPosition + 1);

                    }
                    return true;
                }

            });

            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    int itemType = ExpandableListView.getPackedPositionType(id);

                    if (itemType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                        int groupPosition = ExpandableListView.getPackedPositionGroup(id);
                        mainActivity.newIdeaDialog(groupPosition + 1);
                        return true;

                    } else {
                        // null item; we don't consume the click
                        return false;
                    }
                }
            });

        }

    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {


        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            return ListFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Ideas";
                case 1:
                    return "Later";
                case 2:
                    return "Done";
            }
            return null;
        }
    }

}
