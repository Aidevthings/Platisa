# Hosting Guide: Private vs Public Repository

You mentioned your repository is **Private**.
**Important:** GitHub Pages (the free website hosting) **does not work on Private repositories** unless you pay for GitHub Pro.

You have two options:

## Option A: Keep Your Code Private (Recommended)
If you want to keep your app's source code hidden from the world, **do not make your main repository public.**
Instead, create a **separate** repository just for the legal files.

1.  Create a **new** repository on GitHub named `platisa-legal` (or similar).
2.  Make sure this new repository is **Public**.
3.  Upload **only** your `privacy_policy.html` file to it.
4.  Go to **Settings > Pages** in that new repository.
5.  Set the source to `main` branch.
6.  Your policy will be online, and your app code stays private in your other repository.

---

## Option B: Make Your Main Repository Public
If you don't mind anyone seeing your source code (open source), you can change your existing repository to Public.

1.  Go to your repository on GitHub.
2.  Click **Settings** (Top bar).
3.  Scroll all the way down to the **"Danger Zone"** section.
4.  Click **Change repository visibility**.
5.  Select **Change to public**.
6.  You will need to confirm this action.
7.  Once Public, the **Pages** tab will appear in the sidebar under "Code and automation".

---

## After Hosting
Once you get the link (from either Option A or B), remember to paste it into the "Privacy Policy URL" field in the Google Play Console!
