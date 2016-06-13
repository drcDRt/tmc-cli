package fi.helsinki.cs.tmc.cli.command;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fi.helsinki.cs.tmc.cli.Application;
import fi.helsinki.cs.tmc.cli.io.TestIo;
import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;

import org.junit.Before;
import org.junit.Test;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

public class ListExercisesCommandTest {

    Application app;
    TestIo io;
    TmcCore mockCore;

    @Before
    public void setUp() {
        io = new TestIo();
        app = new Application(io);
        mockCore = mock(TmcCore.class);
        app.setTmcCore(mockCore);

        doAnswer(new Answer<Callable<Course>>() {
            @Override
            public Callable<Course> answer(InvocationOnMock invocation) throws Throwable {
                final Course course = (Course)invocation.getArguments()[1];
                return new Callable<Course>() {
                    @Override
                    public Course call() throws Exception {
                        return course;
                    }
                };
            }
        }).when(mockCore).getCourseDetails(any(ProgressObserver.class), any(Course.class));
    }
    
    @Test
    public void failIfCoreIsNull() {
        app.setTmcCore(null);
        String[] args = {"list-exercises", "foo"};
        app.run(args);
        assertFalse(io.getPrint().contains("Course doesn't exist"));
    }

    @Test
    public void giveMessageIfNoExercisesOnCourse() {
        Callable<List<Course>> callable = new Callable<List<Course>>() {
            @Override
            public List<Course> call() throws Exception {
                return Arrays.asList(new Course("test-course123"));
            }
        };
        when(mockCore.listCourses(any(ProgressObserver.class))).thenReturn(callable);

        String[] args = {"list-exercises", "test-course123"};
        app.run(args);
        assertThat(io.out(), containsString("have any exercises"));
    }

    @Test
    public void listExercisesGivesCorrectExercises() {
        Callable<List<Course>> callable = new Callable<List<Course>>() {
            @Override
            public List<Course> call() throws Exception {
                List<Exercise> list = Arrays.asList(
                        new Exercise("hello-exerciseNames"),
                        new Exercise("cool-exerciseNames"));

                Course course = new Course("test-course123");
                course.setExercises(list);

                return Arrays.asList(course);
            }
        };
        when(mockCore.listCourses(any(ProgressObserver.class))).thenReturn(callable);

        String[] args = {"list-exercises", "test-course123"};
        app.run(args);
        assertThat(io.out(), containsString("hello-exerciseNames"));
    }

    @Test
    public void emptyArgsGivesAnErrorMessage() {
        when(mockCore.listCourses(any(ProgressObserver.class))).thenReturn(null);
        String[] args = {"list-exercises"};
        app.run(args);
        assertThat(io.out(), containsString("No course specified"));
    }

    @Test
    public void failIfCourseExists() {
        Callable<List<Course>> callable = new Callable<List<Course>>() {
            @Override
            public List<Course> call() throws Exception {
                return Arrays.asList(new Course("hello-exerciseNames"));
            }
        };
        when(mockCore.listCourses(any(ProgressObserver.class))).thenReturn(callable);

        String[] args = {"list-exercises", "abc"};
        app.run(args);
        assertThat(io.out(), containsString("Course doesn't exist"));
    }
}
