<script lang="ts">
	import { cubicOut } from 'svelte/easing';
	import { Tween } from 'svelte/motion';
	import { prefersReducedMotion } from 'svelte/motion';

	let { children } = $props();
	let rotateX = new Tween(0, { easing: cubicOut });
	let rotateY = new Tween(0, { easing: cubicOut });
	let el: HTMLDivElement;

	const move = (e: any) => {
		if (prefersReducedMotion.current) {
			rotateX.set(0);
			rotateY.set(0);
		}
		const rect = el.getBoundingClientRect();
		let x = e.clientX - rect.left - rect.width / 2;
		let y = e.clientY - rect.top - rect.height / 2;

		let force = 20;
		let rx = (-x / rect.width) * force;
		let ry = (-y / rect.height) * -force;

		rotateX.set(ry);
		rotateY.set(rx);
	};
</script>

<div class="perspective-normal">
	<div
		bind:this={el}
		onmousemove={move}
		onmouseleave={() => (rotateX.set(0), rotateY.set(0))}
		class="w-full h-full rounded-lg shadow-lg transition-transform duration-300 ease-out will-change-transform hover:scale-105"
		style={`transform: rotateX(${rotateX.current}deg) rotateY(${rotateY.current}deg);`}
	>
		{@render children?.()}
		<div
			class="absolute inset-0 rounded-lg pointer-events-none"
			style={`background: radial-gradient(circle at ${50 - rotateY.current * 5}% ${50 + rotateX.current * 5}%, rgba(255,255,255,0.4), transparent 80%);`}
		></div>
	</div>
</div>
