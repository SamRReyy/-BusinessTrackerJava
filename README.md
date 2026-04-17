# Business Tracker - Android Application

![App Logo](app/src/main/res/drawable/app_logo.png)

## I. Project Overview
**Executive Summary**  
Business Tracker is a comprehensive financial management tool designed for micro-entrepreneurs and small business owners. The application centralizes the management of multiple business ventures, allowing users to monitor real-time profit and loss, track granular expenses and sales, and manage operational tasks within a single, cohesive dashboard.

**Objectives**
*   **Centralized Tracking:** Unified platform for multiple businesses.
*   **Real-Time Insights:** Instant visibility into financial health via data visualization.
*   **Data Mobility:** Secure cloud synchronization via Firebase.
*   **Professional Reporting:** Programmatic generation of multi-page PDF reports.

---

## II. Functional Specifications

### Core Features
1.  **Multi-Business Dashboard:** View aggregate stats (Total Revenue, Net Profit, Total Budget) and visual charts.
2.  **Granular Management:** Dedicated screens for each business to manage:
    *   **Expenses:** Log business costs with category and date tracking.
    *   **Sales:** Record revenue and monitor income streams.
    *   **Tasks:** Operational to-do lists with automated reminders.
3.  **Download PDF Reports:** Generate and upload professional business summaries to Google Drive.
4.  **Privacy Mode:** Mask sensitive financial values on the dashboard for safe use in public.
5.  **Smart Notifications:** Background alerts for over-budget scenarios and task deadlines.
6.  **Dark Mode:** Full support for system-wide dark theming.

---

## III. Technical Specifications

### Tech Stack
*   **IDE:** Android Studio
*   **Language:** Native Java
*   **Minimum SDK:** API 24 (Android 7.0)
*   **Target SDK:** API 34 (Android 14)

### Architecture & Design Patterns
*   **Design Pattern:** Singleton (`StorageService`) for centralized state management.
*   **Communication:** Observer Pattern via custom listeners for real-time UI updates.
*   **UI Binding:** ViewBinding for type-safe interaction with XML layouts.
*   **Background Processing:** WorkManager for deferred, reliable task scheduling.

### Integrated APIs & SDKs
| Category | API / SDK | Purpose |
| :--- | :--- | :--- |
| **Backend** | Firebase Auth / Firestore | Authentication and real-time cloud database. |
| **Identity** | Google Sign-In | Frictionless user onboarding. |
| **Productivity** | Google Drive / Calendar | Report storage and task synchronization. |
| **Security** | reCAPTCHA Enterprise | Protection against automated bot attacks. |
| **Communication** | EmailJS (REST) | Identity verification and password reset codes. |
| **Visualization** | MPAndroidChart | Rendering of Bar and Pie charts. |
| **Media** | Glide | Efficient image loading and caching. |
| **PDF Engine** | iTextG | Local generation of professional PDF documents. |

---

## IV. UI/UX Design
*   **Design System:** Material Design 3 (M3) guidelines.
*   **Theming:** Dynamic Light/Dark mode support.
*   **Navigation:** Intuitive combination of Bottom Navigation for main sections and a Tabbed Interface for Profile management.
*   **Accessibility:** High-contrast color palettes and optimized touch targets (minimum 48dp).

---

## V. Development Methodology
The project utilized the **Agile Development Methodology**, allowing for iterative improvements. Key features were prioritized and refined through continuous testing cycles, ensuring a robust user experience and efficient risk management.

---

## VI. Appendices

### Glossary
*   **CRUD:** Create, Read, Update, Delete.
*   **JSON:** JavaScript Object Notation (used for data serialization).
*   **NoSQL:** Non-relational database used for cloud scaling (Firestore).

### External Links
*   [Google Material Design 3](https://m3.material.io/)
*   [Firebase Documentation](https://firebase.google.com/docs)
*   [MPAndroidChart Documentation](https://github.com/PhilJay/MPAndroidChart)

---
*Version 1.0.0 | © 2023 Business Tracker Team*
