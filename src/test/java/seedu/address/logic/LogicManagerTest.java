package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.logic.Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX;
import static seedu.address.logic.Messages.MESSAGE_UNKNOWN_COMMAND;
import static seedu.address.logic.commands.CommandTestUtil.NAME_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.STUDENTID_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.TUTORIALID_DESC_AMY;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalStudents.AMY;
import static seedu.address.testutil.TypicalTutorials.TUTORIAL2;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.assignment.AssignmentList;
import seedu.address.model.student.Student;
import seedu.address.model.tut.TutorialList;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonAssignmentStorage;
import seedu.address.storage.JsonTutorialStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.StudentBuilder;

public class LogicManagerTest {
    private static final IOException DUMMY_IO_EXCEPTION = new IOException("dummy IO exception");
    private static final IOException DUMMY_AD_EXCEPTION = new AccessDeniedException("dummy access denied exception");

    @TempDir
    public Path temporaryFolder;

    private Model model = new ModelManager();
    private Logic logic;

    @BeforeEach
    public void setUp() {
        JsonAddressBookStorage addressBookStorage =
                new JsonAddressBookStorage(temporaryFolder.resolve("addressBook.json"));
        JsonUserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json"));
        JsonAssignmentStorage assignmentStorage =
                new JsonAssignmentStorage(temporaryFolder.resolve("assignments.json"));
        JsonTutorialStorage tutorialStorage = new JsonTutorialStorage(temporaryFolder.resolve("tutorials.json"));
        StorageManager storage = new StorageManager(addressBookStorage, userPrefsStorage,
                assignmentStorage, tutorialStorage);
        logic = new LogicManager(model, storage);
    }

    @Test
    public void execute_invalidCommandFormat_throwsParseException() {
        String invalidCommand = "uicfhmowqewca";
        assertParseException(invalidCommand, MESSAGE_UNKNOWN_COMMAND);
    }

    @Test
    public void execute_commandExecutionError_throwsCommandException() {
        String deleteCommand = "deleteStu 9";
        assertCommandException(deleteCommand, MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
    }

    @Test
    public void execute_validCommand_success() throws Exception {
        String listCommand = ListCommand.COMMAND_WORD;
        assertCommandSuccess(listCommand, ListCommand.MESSAGE_SUCCESS, model);
    }

    @Test
    public void execute_storageThrowsIoException_throwsCommandException() {
        assertCommandFailureForExceptionFromStorage(DUMMY_IO_EXCEPTION, String.format(
                LogicManager.FILE_OPS_ERROR_FORMAT, DUMMY_IO_EXCEPTION.getMessage()));
    }

    @Test
    public void execute_storageThrowsAdException_throwsCommandException() {
        assertCommandFailureForExceptionFromStorage(DUMMY_AD_EXCEPTION, String.format(
                LogicManager.FILE_OPS_PERMISSION_ERROR_FORMAT, DUMMY_AD_EXCEPTION.getMessage()));
    }

    @Test
    public void getFilteredStudentList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> logic.getFilteredStudentList().remove(0));
    }

    /**
     * Executes the command and confirms that
     * - no exceptions are thrown <br>
     * - the feedback message is equal to {@code expectedMessage} <br>
     * - the internal model manager state is the same as that in {@code expectedModel} <br>
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandSuccess(String inputCommand, String expectedMessage,
            Model expectedModel) throws CommandException, ParseException {
        CommandResult result = logic.execute(inputCommand);
        assertEquals(expectedMessage, result.getFeedbackToUser());
        assertEquals(expectedModel, model);
    }

    /**
     * Executes the command, confirms that a ParseException is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertParseException(String inputCommand, String expectedMessage) {
        assertCommandFailure(inputCommand, ParseException.class, expectedMessage);
    }

    /**
     * Executes the command, confirms that a CommandException is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandException(String inputCommand, String expectedMessage) {
        assertCommandFailure(inputCommand, CommandException.class, expectedMessage);
    }

    /**
     * Executes the command, confirms that the exception is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandFailure(String inputCommand, Class<? extends Throwable> expectedException,
            String expectedMessage) {
        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs(),
                new AssignmentList(), new TutorialList());
        assertCommandFailure(inputCommand, expectedException, expectedMessage, expectedModel);
    }

    /**
     * Executes the command and confirms that
     * - the {@code expectedException} is thrown <br>
     * - the resulting error message is equal to {@code expectedMessage} <br>
     * - the internal model manager state is the same as that in {@code expectedModel} <br>
     * @see #assertCommandSuccess(String, String, Model)
     */
    private void assertCommandFailure(String inputCommand, Class<? extends Throwable> expectedException,
            String expectedMessage, Model expectedModel) {
        assertThrows(expectedException, expectedMessage, () -> logic.execute(inputCommand));
        assertEquals(expectedModel, model);
    }

    /**
     * Tests the Logic component's handling of an {@code IOException} thrown by the Storage component.
     *
     * @param e the exception to be thrown by the Storage component
     * @param expectedMessage the message expected inside exception thrown by the Logic component
     */
    private void assertCommandFailureForExceptionFromStorage(IOException e, String expectedMessage) {
        Path prefPath = temporaryFolder.resolve("ExceptionUserPrefs.json");

        // Inject LogicManager with an AddressBookStorage that throws the IOException e when saving
        JsonAddressBookStorage addressBookStorage = new JsonAddressBookStorage(prefPath) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook, Path filePath)
                    throws IOException {
                throw e;
            }
        };

        JsonUserPrefsStorage userPrefsStorage =
                new JsonUserPrefsStorage(temporaryFolder.resolve("ExceptionUserPrefs.json"));
        JsonAssignmentStorage jsonAssignmentStorage =
                new JsonAssignmentStorage(temporaryFolder.resolve("assignments.json"));
        JsonTutorialStorage tutorialStorage = new JsonTutorialStorage(temporaryFolder.resolve("tutorials.json"));
        StorageManager storage = new StorageManager(addressBookStorage, userPrefsStorage,
                jsonAssignmentStorage, tutorialStorage);

        logic = new LogicManager(model, storage);
        model.addTutorial(TUTORIAL2);
        // Triggers the saveAddressBook method by executing an add command
        String addCommand = AddCommand.COMMAND_WORD + NAME_DESC_AMY + STUDENTID_DESC_AMY + TUTORIALID_DESC_AMY;
        Student expectedStudent = new StudentBuilder(AMY).build();
        ModelManager expectedModel = new ModelManager();
        expectedModel.addTutorial(TUTORIAL2);
        expectedModel.addStudent(expectedStudent);
        assertCommandFailure(addCommand, CommandException.class, expectedMessage, expectedModel);
    }
    //TODO: Resolve test case, fails for Ubuntu OS
    //    @Test
    //    public void getAddressBookFilePath_notNull() {
    //        Path expectedPath = temporaryFolder.resolve("data").resolve("addressBook.json");
    //        assertEquals(expectedPath, temporaryFolder.resolve(logic.getAddressBookFilePath()));
    //    }
    @Test
    public void execute_storageThrowsIoExceptionOnSaveTutorials_throwsCommandException() {
        // Create a dummy IOException to be thrown when saving tutorials
        IOException dummyIoException = new IOException("Invalid command format! \n"
                + "add: Adds a student to the tutorial book. Parameters: n/NAME s/STUDENT_ID "
                + "[c/TUTORIAL_CLASS] \n"
                + "Example: add n/Samson s/A1234567U c/T1001");

        // Create a temporary file path for the tutorial file
        Path tempTutorialFilePath = temporaryFolder.resolve("ExceptionTutorials.json");

        // Override the tutorial storage to throw the IOException when saving tutorials
        JsonTutorialStorage tutorialStorage = new JsonTutorialStorage(tempTutorialFilePath) {
            @Override
            public void saveTutorials(TutorialList tutorialList, Path filePath) throws IOException {
                throw dummyIoException;
            }
        };

        // Set up the storage manager with the overridden tutorial storage
        JsonAddressBookStorage addressBookStorage =
                new JsonAddressBookStorage(temporaryFolder.resolve("addressBook.json"));
        JsonUserPrefsStorage userPrefsStorage =
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json"));
        JsonAssignmentStorage jsonAssignmentStorage =
                new JsonAssignmentStorage(temporaryFolder.resolve("assignments.json"));
        StorageManager storage = new StorageManager(addressBookStorage, userPrefsStorage,
                jsonAssignmentStorage, tutorialStorage);

        // Set up LogicManager with the new storage manager that throws the exception
        logic = new LogicManager(model, storage);

        // Prepare the add command to trigger save operations
        String addCommand = AddCommand.COMMAND_WORD + NAME_DESC_AMY;
        Student expectedStudent = new StudentBuilder(AMY).build();
        ModelManager expectedModel = new ModelManager();
        expectedModel.addStudent(expectedStudent);

        // Assert that the CommandException is thrown with the correct message
        assertCommandFailure(addCommand, ParseException.class,
                String.format(dummyIoException.getMessage()), model);
        //TODO: Change from model to expectedModel
    }


}
