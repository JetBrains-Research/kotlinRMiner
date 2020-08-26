import static org.junit.jupiter.api.Assertions.assertEquals;


import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jgit.lib.Repository;
import org.jetbrains.research.kotlinrminer.api.GitHistoryRefactoringMiner;
import org.jetbrains.research.kotlinrminer.api.GitService;
import org.jetbrains.research.kotlinrminer.api.RefactoringHandler;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;


public class TestAllRefactorings {

    @TestFactory
    public Stream<DynamicTest> testAllRefactorings() throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        String jsonFile = System.getProperty("user.dir") + "/src/test/resources/data.json";

        List<CommitData> commits = mapper.readValue(new File(jsonFile),
            mapper.getTypeFactory().constructCollectionType(List.class, CommitData.class));

        return commits
            .stream()
            .map(commitData -> DynamicTest.dynamicTest(
                "Repository: " + commitData.repository + " hash: " + commitData.sha1
                    .substring(0, 5),
                () -> testCommit(commitData)
            ));
    }

    private void testCommit(CommitData data) throws Exception {
        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMiner();
        GitService gitService = new GitService();
        String folder = "tmp" + "/"
            + data.repository
            .substring(data.repository.lastIndexOf('/') + 1, data.repository.lastIndexOf('.'));
        Repository repo = gitService.cloneIfNotExists(folder, data.repository);
        miner.detectAtCommit(repo, data.sha1, new RefactoringHandler() {
            @Override
            public void handle(String commitId,
                List<org.jetbrains.research.kotlinrminer.api.Refactoring> refactorings) {
                Set<String> results = refactorings
                    .stream()
                    .map(org.jetbrains.research.kotlinrminer.api.Refactoring::getName)
                    .collect(Collectors.toSet());
                Set<String> expected = data.refactorings
                    .stream()
                    .map(refactoring -> refactoring.type)
                    .collect(Collectors.toSet());
                assertEquals(expected, results);
            }
        });
    }


    public static class CommitData {

        public String repository;
        public String sha1;
        public List<Refactoring> refactorings;
    }

    public static class Refactoring {
        public String type;
    }

}
