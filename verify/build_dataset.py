#!/usr/bin/env python3
"""
Builds a fully-synthetic WhatsApp group export for GroupMind.

Format (WhatsApp native, DD/MM/YY):  "DD/MM/YY, HH:MM - Sender: Message"
Multi-line messages continue on subsequent lines with no prefix.
A few system lines (encryption notice, "X added Y") are included so the
parser is exercised against real-world noise.

Everything here is invented. No real people, numbers, or data.
The consensus signal is deliberate: the SAME vendors are recommended by
MULTIPLE DISTINCT senders using DIFFERENT phrasings, so semantic retrieval
+ consensus clustering has something real to find.
"""
import random

random.seed(7)

# ---- cast of (synthetic) characters -------------------------------------
SENDERS = [
    "Aarti Sharma", "Vikram Rao", "Neha Iyer", "Rohit Malhotra", "Fatima Sheikh",
    "Deepak Nair", "Priya Menon", "Sanjay Gupta", "Meena Krishnan", "Karthik R",
    "Ritu Bansal", "Zoya Khan", "Manoj Pillai", "Anil (Secretary)", "Suresh 4B",
    "Divya 7C", "Harish Kumar",
]

# System / non-chat lines that a real export contains
SYSTEM_LINES = [
    ("03/05/25", "08:59", None,
     "Messages and calls are end-to-end encrypted. No one outside of this chat, not even WhatsApp, can read or listen to them."),
    ("03/05/25", "09:00", None, "Anil (Secretary) created group \u201cGreenwood Residency A-Wing\u201d"),
    ("03/05/25", "09:01", None, "Anil (Secretary) added Aarti Sharma"),
    ("11/05/25", "19:22", None, "Zoya Khan joined using this group's invite link"),
]

# Each entry: (day, time, sender, text). Assembled per-storyline, then merged
# and sorted chronologically at the end.
messages = []

def add(day, time, sender, text):
    messages.append((day, time, sender, text))

# =========================================================================
# STORYLINE 1 — VENDOR RECOMMENDATIONS  (the demo centerpiece)
# Consensus targets:
#   AC repair  -> "Ramesh Cool Care" / "Ramesh"  (>=4 distinct senders)
#   Electrician-> "Sunil Electricals" / "Sunil"   (>=3 distinct senders)
#   Plumber    -> "Iqbal"                          (>=3 distinct senders)
# Phrasing is deliberately varied so keyword search would MISS the overlap.
# =========================================================================

# --- AC repair thread (spread across weeks, varied wording) ---
add("05/05/25", "13:04", "Priya Menon", "guys my AC not cooling at all in this heat 😭 any good AC repair guy?")
add("05/05/25", "13:11", "Vikram Rao", "Call Ramesh Cool Care, he serviced my split AC last summer. Very reasonable and honest")
add("05/05/25", "13:12", "Vikram Rao", "9x-xxx-xxxx07 (Ramesh). Say you're from Greenwood, he gives society rate")
add("05/05/25", "13:19", "Meena Krishnan", "+1 for ramesh. the cooling mechanic who came for 3B also. did gas refill properly, no issue since")
add("05/05/25", "13:40", "Priya Menon", "thanks will try 🙏")
add("18/05/25", "10:02", "Suresh 4B", "Anyone knows a reliable aircon technician? my bedroom unit making loud noise")
add("18/05/25", "10:15", "Fatima Sheikh", "ramesh cool care 👍 same guy everyone uses. he fixed the noise in our outdoor unit, was just a loose fan")
add("18/05/25", "10:31", "Deepak Nair", "haan ramesh is good. AC service wala number is in the pinned msg i think. otherwise i can share")
add("18/05/25", "10:33", "Suresh 4B", "got it thanks")
add("02/06/25", "16:45", "Divya 7C", "AC guy recommendation pls, ours stopped working today only")
add("02/06/25", "17:01", "Rohit Malhotra", "ramesh — the cooling repair person from cool care. reliable. 9x-xxx-xxxx07")

# --- Electrician thread ---
add("07/05/25", "20:12", "Rohit Malhotra", "power socket in kitchen sparking, scared to use. need electrician urgently")
add("07/05/25", "20:20", "Neha Iyer", "Sunil Electricals. sunil bhai is very good with wiring issues, dont delay with sparking")
add("07/05/25", "20:24", "Karthik R", "yes call sunil, the electrician who did the whole B-wing rewiring. knows our building setup")
add("07/05/25", "20:40", "Rohit Malhotra", "calling him now, thanks")
add("21/05/25", "09:31", "Ritu Bansal", "need someone for electrical work, few switchboards to replace. any suggestion?")
add("21/05/25", "09:44", "Sanjay Gupta", "the wireman sunil is reliable na. did my fan regulators and MCB. charges fair")
add("21/05/25", "09:50", "Manoj Pillai", "sunil electricals +1")

# --- Plumber thread ---
add("12/05/25", "11:15", "Zoya Khan", "bathroom tap leaking continuously, water bill will explode. plumber contact anyone?")
add("12/05/25", "11:29", "Meena Krishnan", "Iqbal bhai. he does all our plumbing, leakage tap etc. honest rate")
add("12/05/25", "11:35", "Harish Kumar", "iqbal the plumber is good yes, he redid my bathroom fitting last month")
add("25/05/25", "18:02", "Divya 7C", "kitchen sink totally choked, water not going down 😩 need plumber")
add("25/05/25", "18:20", "Aarti Sharma", "call iqbal, pipeline/drainage wala. he cleared our blockage in 20 mins")
add("25/05/25", "18:22", "Aarti Sharma", "he also does the overhead tank cleaning if u ever need")

# --- a competing/minority opinion (makes consensus non-trivial) ---
add("02/06/25", "17:10", "Karthik R", "for AC we used a guy called Faizal once, was ok but ramesh is better imo")

# =========================================================================
# STORYLINE 2 — MAINTENANCE NOTICES
# =========================================================================
add("04/05/25", "08:30", "Anil (Secretary)", "NOTICE: Water tank cleaning on Sunday 11th May. Supply will be OFF from 9am to 1pm. Kindly store water in advance.")
add("04/05/25", "08:47", "Neha Iyer", "noted 👍")
add("04/05/25", "09:10", "Suresh 4B", "thanks for the heads up")
add("10/05/25", "19:05", "Anil (Secretary)", "Reminder: tank cleaning tomorrow 9-1. Motors will be switched off.")
add("11/05/25", "13:20", "Anil (Secretary)", "Water supply restored. Tanks cleaned. Report any low pressure to me.")
add("15/05/25", "10:00", "Anil (Secretary)", "Lift in B-wing under maintenance 15th-16th. Please use A-wing lift or stairs. Sorry for inconvenience.")
add("15/05/25", "10:33", "Manoj Pillai", "B wing lift kab tak theek hoga? senior citizens ko problem ho rahi")
add("15/05/25", "10:41", "Anil (Secretary)", "By 16th evening latest. Technician confirmed part will arrive tomorrow morning.")
add("16/05/25", "18:12", "Anil (Secretary)", "B-wing lift is working now. Thanks for patience.")
add("22/05/25", "07:50", "Anil (Secretary)", "Generator servicing today 10am. Backup power unavailable for ~1 hour during the service window.")
add("28/05/25", "20:15", "Anil (Secretary)", "Pest control (common areas + basement) scheduled Saturday 31st, 8am onwards. Flats optional — msg me to opt in for your flat.")
add("28/05/25", "20:40", "Ritu Bansal", "please add 5A for pest control")
add("28/05/25", "20:41", "Fatima Sheikh", "3B also opt in")
add("08/06/25", "09:00", "Anil (Secretary)", "NOTICE: BMC water cut on 12th June (city wide). No municipal supply full day. We'll run on tank reserves, use sparingly.")

# =========================================================================
# STORYLINE 3 — LOST AND FOUND
# =========================================================================
add("06/05/25", "17:45", "Deepak Nair", "Found a bunch of keys near the A-wing lift lobby. Maruti car key + 2 house keys on a red keychain. With the watchman.")
add("06/05/25", "18:30", "Priya Menon", "omg those might be mine! red keychain? checking my bag")
add("06/05/25", "18:52", "Priya Menon", "yes MINE 😅 collecting from watchman thank you so much Deepak!")
add("14/05/25", "08:15", "Zoya Khan", "Lost: kids' blue water bottle (Frozen theme) in the play area yesterday evening. If anyone's child picked up by mistake pls lmk 🙏")
add("14/05/25", "12:40", "Meena Krishnan", "my son had an extra blue bottle today, might be it! will send pic")
add("14/05/25", "12:55", "Zoya Khan", "yes thats the one!! thankuu")
add("20/05/25", "21:10", "Harish Kumar", "Has anyone seen a small brown dog around? Beagle, name Bruno, slipped out of 6C. Very friendly, responds to his name.")
add("20/05/25", "21:14", "Aarti Sharma", "oh no. i'll check the parking and garden side")
add("20/05/25", "21:48", "Karthik R", "Bruno is near the back gate!! watchman is holding him, come quick")
add("20/05/25", "21:52", "Harish Kumar", "GOT HIM 🐶❤️ thank you everyone, panic over")
add("29/05/25", "15:30", "Divya 7C", "Found a black wallet in the staircase between 6-7 floor. Some cards inside, no cash. Handing to Secretary.")
add("29/05/25", "16:05", "Sanjay Gupta", "that could be mine, was looking for it! anil bhai i'll collect from you")

# =========================================================================
# STORYLINE 4 — RULES / POLICY CLARIFICATION
# =========================================================================
add("09/05/25", "11:00", "Vikram Rao", "Quick q — what's the visitor parking rule now? my in-laws visiting for a week, where do they park?")
add("09/05/25", "11:22", "Anil (Secretary)", "Visitor parking: slots V1-V5 near main gate, max 24 hrs. For longer stay pls inform security & me in advance with car number.")
add("09/05/25", "11:25", "Vikram Rao", "perfect thanks")
add("09/05/25", "11:40", "Neha Iyer", "so guests cant use our own numbered slot if we're out of town?")
add("09/05/25", "11:52", "Anil (Secretary)", "You can let a guest use YOUR slot if you inform security. Just can't occupy other residents' slots.")
add("17/05/25", "13:15", "Ritu Bansal", "is the terrace open for use? wanted to do a small birthday for my daughter")
add("17/05/25", "13:40", "Anil (Secretary)", "Terrace can be booked for events. Rs.500 refundable deposit, book via me at least 3 days prior. No loud music after 10pm as per society rule.")
add("17/05/25", "13:44", "Ritu Bansal", "noted, will book for next Sat")
add("23/05/25", "19:30", "Fatima Sheikh", "clarification pls — are pets allowed in the lift? someone objected today")
add("23/05/25", "19:55", "Anil (Secretary)", "Pets ARE allowed in lifts, on a leash, and owners must clean up any mess. This was passed in last AGM. Being a pet-friendly society is society policy.")
add("23/05/25", "20:02", "Harish Kumar", "thank you, some clarity finally 🙏")
add("23/05/25", "20:10", "Meena Krishnan", "good. bruno approves 😄")
add("30/05/25", "10:20", "Suresh 4B", "what are the gym timings? and is there a guest fee?")
add("30/05/25", "10:35", "Anil (Secretary)", "Gym: 5am-10pm daily. Residents free. Guests not permitted in gym for insurance reasons. Please don't share access cards.")
add("05/06/25", "18:00", "Divya 7C", "reminder to everyone — no crackers/loud noise after 10pm please, exam season, kids studying 🙏")

# =========================================================================
# EXTRA VENDOR CHATTER (distractors) — other trades so AC/electrician/plumber
# consensus must be *semantically distinguished*, not just "any vendor".
# =========================================================================
add("08/05/25", "12:10", "Meena Krishnan", "need a good carpenter for wardrobe repair, hinges gone. anyone?")
add("08/05/25", "12:35", "Sanjay Gupta", "Ashok carpenter, does neat work. slightly slow but finishing is good")
add("08/05/25", "12:50", "Divya 7C", "we used Babu for our kitchen cabinets, also fine")
add("13/05/25", "10:05", "Ritu Bansal", "any recommendation for house painting? planning before diwali")
add("13/05/25", "10:30", "Neha Iyer", "we got Colour Craft, quoted reasonable. do get 2-3 quotes though")
add("19/05/25", "09:20", "Fatima Sheikh", "RO water purifier service — mine showing red light. any technician?")
add("19/05/25", "09:44", "Manoj Pillai", "Aquaguard service center number, they send a guy. or local one is Prakash for RO/water purifier")
add("24/05/25", "17:30", "Priya Menon", "looking for a good full-time maid/cook for 2BHK, references welcome 🙏")
add("24/05/25", "17:55", "Aarti Sharma", "will ask my Lakshmi if she knows someone free")
add("27/05/25", "14:15", "Suresh 4B", "gas stove burner not lighting properly, low flame. repair guy?")
add("27/05/25", "14:40", "Vikram Rao", "for gas stove there's a Bajaj service, or the local guy near market. not the same as AC/electric people")
add("01/06/25", "16:00", "Zoya Khan", "anyone knows curtain stitching / tailor for home furnishing?")
add("01/06/25", "16:20", "Divya 7C", "Reshma tailor near the temple, very good with curtains")

# more AC reinforcement (so the marquee query is unambiguous, 5 distinct senders)
add("09/06/25", "11:30", "Neha Iyer", "adding to the AC recommendations — ramesh cool care serviced 3 units for us before summer, spotless work. strongly recommend the ac repair guy")
add("09/06/25", "11:33", "Anil (Secretary)", "Adding Ramesh Cool Care (AC servicing) to the pinned vendor list. Multiple residents vouch for him.")

# =========================================================================
# EXTRA MAINTENANCE
# =========================================================================
add("06/05/25", "08:20", "Anil (Secretary)", "Intercom lines will be down for upgrade on 7th May, 2-4pm. Use mobile numbers to reach security.")
add("12/05/25", "20:30", "Rohit Malhotra", "water pressure very low on 5th floor since evening, anyone else?")
add("12/05/25", "20:38", "Anil (Secretary)", "Checking — one motor tripped, restarting. Should normalize in 30 min.")
add("12/05/25", "21:05", "Rohit Malhotra", "yes pressure back, thanks")
add("18/05/25", "09:00", "Anil (Secretary)", "Parking lines will be re-painted 19th morning. Please move vehicles out of the covered parking by 8am or park on the road side temporarily.")
add("26/05/25", "08:45", "Anil (Secretary)", "CCTV in basement being upgraded today. Some cameras offline till evening, please be alert with your vehicles.")
add("04/06/25", "10:00", "Anil (Secretary)", "Fire safety drill on Saturday 7th, 10am. All residents encouraged to participate. Mock evacuation, don't panic when alarm rings.")
add("04/06/25", "10:20", "Karthik R", "good, we should know the drill honestly")

# =========================================================================
# EXTRA LOST & FOUND
# =========================================================================
add("07/05/25", "22:10", "Manoj Pillai", "Lost my black umbrella in the lobby today, big golf type. if found pls keep with watchman")
add("16/05/25", "13:00", "Priya Menon", "Found a phone charger (white, type-C) in the clubhouse. with me, 2C.")
add("22/05/25", "19:45", "Ritu Bansal", "there's a friendly grey cat hanging near C-wing, has a collar, seems someone's pet. anyone missing a cat?")
add("22/05/25", "19:58", "Neha Iyer", "that's the Menons' cat from 7A i think, they let it roam")
add("31/05/25", "17:20", "Karthik R", "Lost: kids cycle (red BMX) from the parking, was there yesterday. please check if anyone moved it")
add("31/05/25", "18:10", "Suresh 4B", "saw the watchman shift some cycles to the corner for cleaning, check there")
add("31/05/25", "18:25", "Karthik R", "found it there, phew. thanks")

# =========================================================================
# EXTRA POLICY / RULES
# =========================================================================
add("10/05/25", "12:00", "Zoya Khan", "moving in this weekend — any rule for using the lift for shifting furniture?")
add("10/05/25", "12:20", "Anil (Secretary)", "For move-in/move-out: inform security a day prior, use service lift only, and there's a Rs.1000 refundable deposit against damage. Shifting allowed 8am-6pm only.")
add("14/05/25", "09:10", "Fatima Sheikh", "what time is garbage collection now? sometimes i miss it")
add("14/05/25", "09:25", "Anil (Secretary)", "Wet & dry waste collected daily 8-9am at your door. Please segregate — green bin wet, blue bin dry. Housekeeping won't take mixed waste.")
add("20/05/25", "15:40", "Divya 7C", "is there EV charging point in the society? planning to buy an electric scooter")
add("20/05/25", "16:05", "Anil (Secretary)", "One EV charging point near basement gate B. Chargeable per unit, log usage in the register. Committee discussing adding more points.")
add("28/05/25", "11:15", "Sanjay Gupta", "how to book the water tanker if supply is short? city cut is coming on 12th")
add("28/05/25", "11:30", "Anil (Secretary)", "Tanker booking goes through the society office — msg me quantity needed, cost is shared among users of that tanker. Book by 10th for the 12th cut.")

# =========================================================================
# FILLER / NOISE  (good-morning forwards, acknowledgements, small talk)
# Makes retrieval realistic: lots of low-signal messages to sift through.
# =========================================================================
FILLER = [
    "Good morning all 🌸🙏 Have a blessed day",
    "🙏🙏",
    "noted",
    "ok thanks",
    "👍",
    "Thank you 🙏",
    "Anyone else's internet down? Airtel not working since morning",
    "yes mine also down, restarted router still nothing",
    "it's back now",
    "Happy to help 😊",
    "congrats!! 🎉",
    "welcome to the group Zoya 🙏",
    "thanks everyone",
    "please avoid forwarding fake news in the group 🙏",
    "😂😂😂",
    "who left the tap running in the common washroom 2nd floor",
    "sorry that was me, fixed",
    "milk delivery not come today for anyone?",
    "kaam ho gaya?",
    "great initiative 👏",
    "can we please keep the group only for society matters",
    "agreed",
    "Diwali cleaning drive volunteers needed, ping me",
    "count me in",
    "watchman on leave tomorrow, backup guard coming",
    "Amazon delivery guy asking for A-wing 5th floor, can someone confirm gate",
    "swiggy stuck at gate, please allow 🙏",
    "any tuition teacher recommendation for class 8 maths?",
    "society maintenance bill for this quarter shared on email, pls check",
    "has everyone paid the maintenance? last date is 15th",
    "reminder: monthly committee meeting Sunday 6pm in clubhouse",
    "can we get one more dustbin near the play area",
    "kids playing cricket near parked cars again, someone's mirror broke",
    "please ask your kids to play in the designated area only 🙏",
    "loud construction noise from 8B whole day, is it allowed on weekends?",
    "renovation timings are weekdays 9-6 only i think, check with secretary",
    "beautiful rangoli at the entrance today 😍 who made it?",
    "that was the ladies group for the festival 🙏",
    "happy birthday to little Aarav 🎂🎉",
    "🎂🎉🎉",
    "does anyone have a spare gas cylinder, mine over and booking delayed",
    "the stray dogs near gate are being fed, someone pls coordinate feeding spot away from entrance",
    "newspaper vendor changed timings, now comes by 7am",
    "guest lecture on terrace gardening this Sunday, all welcome 🌱",
    "AC of the clubhouse also not working btw 😅",
    "election of new committee members next month, nominations open",
    "please switch off corridor lights during daytime, saving electricity",
    "someone parked in my slot A-23 again 😤",
    "kindly do not park in others' slots, use visitor parking",
    "thank you all for the quick help today, great community 🙏",
    "is the swimming pool open this season?",
    "pool opens next week after cleaning, timings will be shared",
    "lovely evening, kids enjoying in the garden 😊",
    "please keep the staircase clear, no shoe racks outside doors (fire safety)",
    "who is the current secretary's backup if anil is unavailable?",
    "monsoon coming, please clear your balcony drains to avoid seepage",
    "great, thanks for organizing 👏",
    "anyone travelling to airport tomorrow morning, can share cab?",
    "haan me bhi 6am flight, can share",
    "test message pls ignore",
    "received 🙏",
]
FILLER_DAYS = ["03/05/25","05/05/25","06/05/25","08/05/25","11/05/25","13/05/25",
               "15/05/25","16/05/25","19/05/25","21/05/25","24/05/25","26/05/25",
               "29/05/25","31/05/25","02/06/25","03/06/25","06/06/25","07/06/25",
               "09/06/25","10/06/25","13/06/25","14/06/25"]

TARGET_FILLER = 95  # sample with replacement to bulk up realistic noise
for i in range(TARGET_FILLER):
    txt = FILLER[i % len(FILLER)] if i < len(FILLER) else random.choice(FILLER)
    day = random.choice(FILLER_DAYS)
    hh = random.randint(6, 22)
    mm = random.randint(0, 59)
    add(day, f"{hh:02d}:{mm:02d}", random.choice(SENDERS), txt)

# =========================================================================
# ASSEMBLE  — sort chronologically, format, write file
# =========================================================================
def sort_key(m):
    day, time, sender, text = m
    d, mo, y = day.split("/")
    hh, mm = time.split(":")
    return (int("20"+y), int(mo), int(d), int(hh), int(mm))

# system lines carry sender=None
all_lines = [(d, t, s, x) for (d, t, s, x) in SYSTEM_LINES] + messages
all_lines.sort(key=sort_key)

out = []
for day, time, sender, text in all_lines:
    if sender is None:
        out.append(f"{day}, {time} - {text}")
    else:
        # keep multi-line messages joined with real newlines (no prefix on cont.)
        out.append(f"{day}, {time} - {sender}: {text}")

with open("/home/claude/groupmind/data/greenwood_residency_chat.txt", "w", encoding="utf-8") as f:
    f.write("\n".join(out) + "\n")

# quick stats
real = [m for m in all_lines if m[2] is not None]
print(f"Total lines written : {len(all_lines)}")
print(f"  chat messages     : {len(real)}")
print(f"  system lines      : {len(all_lines) - len(real)}")
print(f"Distinct senders    : {len(set(m[2] for m in real))}")
print(f"Date span           : {all_lines[0][0]} -> {all_lines[-1][0]}")
