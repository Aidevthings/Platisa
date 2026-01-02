# How to Upload to GitHub using GitHub Desktop

**STOP! Read this before you click "Create repository" in your screenshot.**

I noticed in your screenshot that the path says:
`A:\Software Dev\Platisa\Platisa`

**Problem:** This will create a new empty folder *inside* your existing project. We don't want that! We want to turn your *existing* folder into a repository.

## Step 1: Add Your Existing Folder
1.  Cancel the "Create a new repository" window you currently have open.
2.  In GitHub Desktop, go to **File** (top left) -> **Add local repository...**
3.  Click **Choose...** and select your main folder: `A:\Software Dev\Platisa`.
4.  Click **Add repository**.
    *   *Note: It might say "This directory does not appear to be a Git repository."*
    *   Click the blue link that says **create a repository here**.
5.  Now the "Create a New Repository" window appears again, but the path will be correct (`A:\Software Dev\Platisa` without the double name).
6.  Click **Create repository**.

## Step 2: Publish (Upload) to GitHub
1.  You should now see your project in the main view.
2.  Click the blue button **Publish repository** (top main bar).
3.  **Name**: `Platisa`
4.  **Description**: `Placanje Racuna`
5.  **Keep this code private**:
    *   **CHECK this box** if you want your code to be secret. (Remember: You can't host the Privacy Policy for free if it's private).
    *   **UNCHECK this box** if you want it to be Public (Open Source) and enable free Privacy Policy hosting immediately.
6.  Click **Publish repository**.

## Step 3: Hosting the Privacy Policy
If you chose **Public**:
1.  Go to your Repository Settings on GitHub.com.
2.  Go to **Pages**.
3.  Select source: **Deploy from a branch** -> `main` -> `/docs` folder.

If you chose **Private**:
1.  You must create a **separate** public repository just for the `privacy_policy.html` file (as explained in the previous guide).
