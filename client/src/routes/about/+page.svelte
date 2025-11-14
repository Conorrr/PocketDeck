<script lang="ts">
	import { onMount } from 'svelte';

	const questions = [
		{
			anchor: 'what-is-this',
			question: 'What is this?',
			answer: `PocketDeck is a site for sharing card decks for <a href="https://tcgpocket.pokemon.com/">Pokemon TCG Pocket</a>. PocketDeck's trick is that it can convert screenshots of decks to card lists.`
		},
		{
			anchor: 'how-does-it-work',
			question: 'How does the deck screenshot converter work?',
			answer: `The deck screenshot converter processes your image in a few clear steps. It starts by using OpenCV to clean up the screenshot and identify where the cards are located, with Canny edge detection helping outline the card shapes. Once the card areas are found, the system compares each card to a database of known images using a combination of PHash and ORB to make accurate matches. Uploaded images are only kept temporarily and may be reviewed to improve accuracy or fix issues, but they aren't stored long term. If you want more details or are interest in the source code feel free to reach out.`
		},
		{
			anchor: 'who-built-pocketcards',
			question: 'Who built PocketCards?',
			answer: `PocketCards was built by a UK-based developer who got tired of manually typing out deck lists and figured there had to be a better way. It's just a personal project made to help other players save time.`
		},
		{
			anchor: 'why',
			question: 'Why create PocketDeck?',
			answer: `PocketDeck was created to make it much easier to view and understand decks at a glance, especially when they include lesser-known cards. Screenshots don't always show important details clearly, so the goal was to build a tool that pulls up everything you'd want to know—what pack a card is from, its abilities and attacks, energy costs, and more. It also makes sharing decks far simpler than passing around images. Another motivation behind the project was an interest in computer vision.`
		},
		{
			anchor: 'shiny-cards',
			question: 'Where are my shiny cards?',
			answer: `It's deliberate. The tool maps cards to their first and lowest rarity form. The idea being that it's easier to read and identify cards. Sorry the decks now looks less flashy.`
		},
		{
			anchor: 'upcoming-features',
			question: 'What changes are in the pipeline?',
			answer: `Lots... here are a few:
            <ul>
                <li>Ability to edit decks - easier to suggest changes or fix issues with the computer vision</li>
                <li>Ability to create decks from scratch</li>
                <li>Create and share collections of decks</li>
                <li>Additional card languages</li>
                <li>Improve accuracy</li>
            </ul>`
		},
		{
			anchor: 'files',
			question: 'What happens to the files I upload?',
			answer: `They are stored temporarily, I may use them for debugging or to help refine and improve the image recognition.`
		},
		{
			anchor: 'privacy',
			question: 'Do you collect any other data?',
			answer: `There is some basic web analytics on the site to help me see how many visitors I have and where they come from. But nothing personal is stored (IP adresses etc.). None of this is shared with anyone else (E.G. Google Analytics). I don't even use cookies to track users betwen visits.`
		},
		{
			anchor: 'contact',
			question: 'How can I contact the creator of PocketDeck?',
			answer: `Feel free to <a class="email" href="#email">email me</a> or drop me a message <a href="/contact">here</a>. It's great to hear feedback, questions and sugguestions.`
		},
		{
			anchor: 'problem-recognising',
			question: 'There is a problem recognising my deck',
			answer: `Unfortunately that happens sometimes. Please <a class="email" href="#">email me</a> or drop me a message <a href="/contact">here</a>, all feedback helps me improve the image recognition.`
		}
	];

	onMount(() => {
		const links = document.querySelectorAll('.answer .email');

		links.forEach((link) => {
			if (link instanceof HTMLAnchorElement) {
				link.addEventListener('mouseenter', () => {
					const hostname = 'restall.me';
					const enquiries = 'pocketdeck';
					const at = String.fromCharCode(Math.pow(2, 6));
					const linked = 'mai' + 'lto:' + enquiries + at + hostname;
					link.href = linked;
				});

				link.addEventListener('mouseleave', () => {
					link.href = '#';
				});
			}
		});
	});
</script>

<div
	class="flex items-center justify-between mb-4 opacity-90 bg-white text-slate-900 p-2 pl-3 rounded-xl shadow-md border border-slate-300/50"
>
	<section aria-label="Q&A" class="mx-auto p-4">
		<h2 class="text-2xl font-semibold mb-4">About</h2>

		<div id="qa-list" class="space-y-3" role="list">
			{#each questions as question}
				<div class="rounded-lg bg-slate-200 p-3" id={question.anchor}>
					<span class="block text-sm font-medium">
						{question.question}
					</span>
					<p class="text-sm text-gray-700 answer">
						{@html question.answer}
					</p>
				</div>
			{/each}
		</div>
	</section>
</div>

<style>
	:global(.answer a) {
		font-weight: bold;
		text-decoration: underline;
	}
	:global(.answer li) {
		list-style-type: circle;
		margin-left: 2em;
	}
</style>
