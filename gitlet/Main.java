package gitlet;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Xiaoyi Zhu
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        try {
            if (args.length == 0) {
                Utils.message("Please enter a command.");
                System.exit(0);
            }
            if (!VALIDCOMMAND.contains(args[0])) {
                Utils.message("No command with that name exists.");
                System.exit(0);
            }
            if (!checkNumberOfOperand(args)) {
                Utils.message("Incorrect operands.");
                System.exit(0);
            }
            if (args[0].equals("init") && checkInit()) {
                Utils.message("A Gitlet version-control system already"
                        + " exists in the current directory.");
                System.exit(0);
            }
            if (!args[0].equals("init")
                    && VALIDCOMMAND.contains(args[0]) && !checkInit()) {
                Utils.message("Not in an initialized Gitlet directory.");
                System.exit(0);
            }
            if (args.length == 1 && args[0].equals("init") && !checkInit()) {
                repo = new Repo();
                File hist = new File(".gitlet/myrepo");
                Utils.writeObject(hist, repo);
            }
            if (checkInit()) {
                repo = getHist();
                if (args.length == 1) {
                    callMethod1(args[0]);
                } else {
                    callMethod2(args);
                }
                File hist = new File(".gitlet/myrepo");
                Utils.writeObject(hist, repo);
            }
        } catch (GitletException e) {
            System.exit(0);
        }
    }

    /** Check the number of operands.
     * @param command commands from input.
     * @return whether it is valid. */
    private static boolean checkNumberOfOperand(String[] command) {
        int num = command.length;
        if (command[0].equals("checkout") || command[0].equals("commit")) {
            return true;
        }
        return (num == 1 || !ONEOPERAND.contains(command[0]))
                && (num == 2 || !TWOOPERAND.contains(command[0])) && num < 4;
    }

    /** Check the status of the repo.
     * @return whether there is a repo already. */
    private static boolean checkInit() {
        return Files.exists(Paths.get(".gitlet"));
    }

    /** Call method that requires no argument.
     *@param command the according method name. */
    private static void callMethod1(String command) {
        switch (command) {
        case "commit" :
            Utils.message("Please enter a commit message.");
            System.exit(0);
            break;
        case "log" :
            repo.log(repo.getHead());
            break;
        case "global-log" :
            repo.goballog();
            break;
        case "status" :
            repo.status();
            break;
        default:
        }
    }

    /** Call method that requires argument(s).
     *@param command the according method name. */
    private static void callMethod2(String[] command) {
        switch (command[0]) {
        case "add" :
            repo.add(command[1]);
            break;
        case "commit" :
            repo.commit(command[1]);
            break;
        case "rm" :
            repo.rm(command[1]);
            break;
        case "find" :
            repo.find(command[1]);
            break;
        case "branch" :
            repo.branch(command[1]);
            break;
        case "checkout" :
            if (command.length == 3 && command[1].equals("--")) {
                repo.checkout1(command[2], repo.getHead());
            } else if (command.length == 4 && command[2].equals("--")) {
                repo.checkout2(command[1], command[3]);
            } else if (command.length == 2) {
                repo.checkout3(command[1]);
            } else {
                Utils.message("Incorrect operands.");
                System.exit(0);
            }
            break;
        case "reset" :
            repo.reset(command[1]);
            break;
        case "rm-branch" :
            repo.rmBranch(command[1]);
            break;
        case "merge" :
            repo.merge(command[1]);
            break;
        default:
        }
    }

    /** recover the status from last commit.
     * @return get the history from this repo. */
    private static Repo getHist() {
        File hist = new File(".gitlet/myrepo");
        return Utils.readObject(hist, Repo.class);
    }

    /** Array of valid commands. */
    private static final List<String> VALIDCOMMAND =
            new LinkedList<>(Arrays.asList("init", "add", "commit",
            "rm", "log", "global-log", "find", "status", "checkout", "branch",
            "rm-branch", "reset", "merge"));

    /** Array of valid commands requires one operand. */
    private static final List<String> ONEOPERAND =
            new LinkedList<>(Arrays.asList("init", "log", "global-log",
                    "status", "commit"));

    /** Array of valid commands requires two operand. */
    private static final List<String> TWOOPERAND =
            new LinkedList<>(Arrays.asList("add", "commit",
                    "rm", "find", "checkout", "branch",
                    "rm-branch", "reset", "merge"));

    /** The repo object. */
    private static Repo repo;

}
