<script lang="ts">
	var email = $state('#');
	function emailHover() {
		const hostname = 'restall.me';
		const enquiries = 'pocketdeck';
		const at = String.fromCharCode(Math.pow(2, 6));
		const linked = 'mai' + 'lto:' + enquiries + at + hostname;
		email = linked;
	}
	function resetEmail() {
		email = '#';
	}

	let status = $state('');

	const handleSubmit = async (evt: SubmitEvent) => {
		evt.preventDefault();
		status = 'Submitting...';
		const formData = new FormData(evt.currentTarget as HTMLFormElement);
		const object = Object.fromEntries(formData);
		const json = JSON.stringify(object);

		const response = await fetch('https://api.web3forms.com/submit', {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json',
				Accept: 'application/json'
			},
			body: json
		});
		const result = await response.json();
		if (result.success) {
			status = "Thank you for reaching out, I'll be in contact soon.";
		}
	};
</script>

<div
	class="flex flex-col items-center justify-between mb-4 opacity-90 bg-white text-slate-900 p-2 pl-3 rounded-xl shadow-md border border-slate-300/50"
>
	<section aria-label="blurb" class="mx-auto p-4 w-full">
		<h2 class="text-2xl font-semibold mb-4 text-slate-900">Contact</h2>
		<p>
			If you have a problem, some feedback, a question or just want to say hi I'd love to hear from
			you. Fill out the form below or drop me
			<a onmouseenter={emailHover} onmouseleave={resetEmail} href={email}>an email</a>
		</p>
	</section>

	<section aria-label="contact form" class="p-8">
		{#if !status}
			<form
				onsubmit={handleSubmit}
				method="post"
				action="https://api.web3forms.com/submit"
				class="space-y-4 bg-white"
			>
				<input type="hidden" name="access_key" value="f04edab2-a104-4685-bd9c-0cb5ab7338eb" />

				<input
					type="text"
					name="name"
					required
					placeholder="Your name"
					class="w-full px-3 py-2 rounded-lg border border-slate-300 focus:outline-none focus:ring-2 focus:ring-slate-500 bg-slate-50"
				/>

				<input
					type="email"
					name="email"
					required
					placeholder="Your email"
					class="w-full px-3 py-2 rounded-lg border border-slate-300 focus:outline-none focus:ring-2 focus:ring-slate-500 bg-slate-50"
				/>

				<textarea
					name="message"
					required
					rows="3"
					placeholder="Your message"
					class="w-full px-3 py-2 rounded-lg border border-slate-300 focus:outline-none focus:ring-2 focus:ring-slate-500 bg-slate-50"
				></textarea>

				<button
					type="submit"
					class="w-full bg-slate-600 hover:bg-slate-700 text-white font-medium py-2 px-4 rounded-md transition-colors"
				>
					Send Message
				</button>
			</form>
		{:else}
			<div class="text-md text-slate-900" class:animate-pulse={status=='Sending...'}>{status}</div>
		{/if}
	</section>
</div>

<style>
	a {
		font-weight: bold;
		text-decoration: underline;
	}
</style>
