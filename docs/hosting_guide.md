# How to Host Your Privacy Policy on GitHub Pages

Since you couldn't find the "Pages" tab, it's likely you haven't uploaded your code to GitHub yet. Follow these steps to set everything up from scratch.

## Step 1: Create a Repository on GitHub
1.  Log in to [GitHub.com](https://github.com).
2.  Click the **+** icon in the top-right corner and select **New repository**.
3.  **Repository name**: Enter `platisa-privacy` (or just `Platisa`).
4.  **Visibility**: Choose **Public** (Required for free GitHub Pages).
5.  **Initialize**: *Do not* check "Add a README" or .gitignore.
6.  Click **Create repository**.
7.  Copy the HTTPS URL provided (e.g., `https://github.com/YourUsername/platisa-privacy.git`).

## Step 2: Upload Your Code (Terminal)
Open your terminal in the project folder (`A:\Software Dev\Platisa`) and run these commands one by one:

```powershell
# 1. Initialize Git
git init

# 2. Add your files
git add .

# 3. Commit your changes
git commit -m "Initial commit with Privacy Policy"

# 4. Link to GitHub (Replace URL with YOUR repo URL from Step 1)
git remote add origin https://github.com/YourUsername/platisa-privacy.git

# 5. Push the code
git push -u origin master
```
*(Note: If the last command fails asking for a password, you may need to set up a Personal Access Token or use GitHub Desktop).*

## Step 3: Enable GitHub Pages
Once your code is pushed:
1.  Go to your repository page on GitHub.
2.  Click **Settings** (Top navigation bar, right side).
3.  In the left sidebar, under the "Code and automation" section, click **Pages**.
4.  **Source**: Select **Deploy from a branch**.
5.  **Branch**: Select `master` (or `main`) and folder `/docs`.
6.  Click **Save**.

## Step 4: Get Your Link
Wait about 1-2 minutes. Refresh the Pages settings page. You will see a banner:
> "Your site is live at `https://YourUsername.github.io/platisa-privacy/`"

**Click that link**, then add `/privacy_policy.html` to the end of the URL to see your policy.
**Example**: `https://YourUsername.github.io/platisa-privacy/privacy_policy.html`
