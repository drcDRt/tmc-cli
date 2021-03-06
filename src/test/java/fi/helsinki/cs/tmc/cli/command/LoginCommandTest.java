package fi.helsinki.cs.tmc.cli.command;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import fi.helsinki.cs.tmc.cli.Application;
import fi.helsinki.cs.tmc.cli.backend.Account;
import fi.helsinki.cs.tmc.cli.backend.AccountList;
import fi.helsinki.cs.tmc.cli.backend.CourseInfo;
import fi.helsinki.cs.tmc.cli.backend.SettingsIo;
import fi.helsinki.cs.tmc.cli.backend.TmcUtil;
import fi.helsinki.cs.tmc.cli.core.CliContext;
import fi.helsinki.cs.tmc.cli.io.TestIo;

import fi.helsinki.cs.tmc.core.TmcCore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SettingsIo.class, TmcUtil.class})
public class LoginCommandTest {

    private static final String SERVER = "testserver";
    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "testpassword";

    private Application app;
    private CliContext ctx;
    private TestIo io;
    private TmcCore mockCore;

    @Before
    public void setUp() {
        io = new TestIo();
        mockCore = mock(TmcCore.class);

        ctx = spy(new CliContext(io, mockCore));
        app = new Application(ctx);

        mockStatic(TmcUtil.class);
        mockStatic(SettingsIo.class);
        when(TmcUtil.hasConnection(eq(ctx))).thenReturn(true);
        when(SettingsIo.loadAccountList()).thenReturn(new AccountList());
        when(SettingsIo.saveAccountList(any(AccountList.class))).thenReturn(true);
    }

    @Test
    public void failIfBackendFails() {
        app = new Application(ctx);
        when(ctx.loadBackendWithoutLogin()).thenReturn(false);

        String[] args = {"login", "-s", SERVER, "-u", USERNAME, "-p", PASSWORD};
        app.run(args);
        io.assertNotContains("Login successful");
    }

    @Test
    public void failIfThereIsNoConnection() {
        when(TmcUtil.hasConnection(eq(ctx))).thenReturn(false);

        String[] args = {"login"};
        app.run(args);
        io.assertContains("don't have internet connection");
    }

    @Test
    public void logsInWithCorrectServerUserAndPassword() {
        when(TmcUtil.tryToLogin(eq(ctx), any(Account.class))).thenReturn(true);
        when(SettingsIo.saveAccountList(any(AccountList.class))).thenReturn(true);
        String[] args = {"login", "-s", SERVER, "-u", USERNAME, "-p", PASSWORD};
        app.run(args);
        io.assertContains("Login successful.");
    }

    @Test
    public void userGetsErrorMessageIfLoginFails() {
        when(TmcUtil.tryToLogin(eq(ctx), any(Account.class))).thenReturn(true);
        when(SettingsIo.saveAccountList(any(AccountList.class))).thenReturn(false);
        String[] args = {"login", "-s", SERVER, "-u", USERNAME, "-p", "WrongPassword"};
        app.run(args);
        io.assertContains("Failed to write the accounts file.");
    }

    @Test
    public void loginAsksUsernameFromUserIfNotGiven() {
        when(SettingsIo.loadAccountList()).thenReturn(new AccountList());
        when(SettingsIo.saveAccountList(any(AccountList.class))).thenReturn(true);
        String[] args = {"login", "-s", SERVER, "-p", PASSWORD};
        io.addLinePrompt(USERNAME);
        app.run(args);
        io.assertAllPromptsUsed();
    }

    @Test
    public void loginAsksPasswordFromUserIfNotGiven() {
        when(TmcUtil.tryToLogin(eq(ctx), any(Account.class))).thenReturn(true);
        String[] args = {"login", "-s", SERVER, "-u", USERNAME};
        io.addPasswordPrompt(PASSWORD);
        app.run(args);
        io.assertAllPromptsUsed();
    }

    @Test
    public void loginAsksServerFromUserIfNotGiven() {
        when(TmcUtil.tryToLogin(eq(ctx), any(Account.class))).thenReturn(true);
        String[] args = {"login", "-p", PASSWORD, "-u", USERNAME};
        io.addLinePrompt(SERVER);
        app.run(args);
        io.assertAllPromptsUsed();
    }

    @Test
    public void serverAndNotAskedAfterLogout() {
        Account account = new Account(SERVER, "username", "pass");
        CourseInfo info = new CourseInfo(account, null);
        when(TmcUtil.tryToLogin(eq(ctx), any(Account.class))).thenReturn(true);
        when(ctx.getCourseInfo()).thenReturn(info);
        String[] args = {"login"};
        io.addPasswordPrompt(PASSWORD);
        app.run(args);
        io.assertAllPromptsUsed();
    }

    @Test
    public void courseInfoValuesOverridedByOptions() {
        Account account = new Account(SERVER, "username", "pass");
        CourseInfo info = new CourseInfo(account, null);
        when(TmcUtil.tryToLogin(eq(ctx), any(Account.class))).thenReturn(true);
        when(ctx.getCourseInfo()).thenReturn(info);
        String[] args = {"login", "-p", PASSWORD, "-u", USERNAME};
        app.run(args);
        io.assertAllPromptsUsed();

        Account expectedAccount = new Account(SERVER, USERNAME, PASSWORD);
        verifyStatic();
        TmcUtil.tryToLogin(eq(ctx), eq(expectedAccount));
    }
}
